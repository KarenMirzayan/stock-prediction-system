package kz.kbtu.webapi.service;

import kz.kbtu.common.entity.*;
import kz.kbtu.webapi.dto.*;
import kz.kbtu.webapi.repository.ArticleRepository;
import kz.kbtu.webapi.repository.GlossaryTermRepository;
import kz.kbtu.webapi.repository.PredictionRepository;
import kz.kbtu.webapi.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EducationService {

    private final GlossaryTermRepository glossaryTermRepository;
    private final QuizRepository quizRepository;
    private final ArticleRepository articleRepository;
    private final PredictionRepository predictionRepository;
    private final OllamaEducationClient ollamaClient;

    // ── Glossary ──────────────────────────────────────────────────────────────

    public List<GlossaryTermDto> getGlossaryTerms(String search) {
        List<GlossaryTerm> terms = (search != null && !search.isBlank())
                ? glossaryTermRepository.findByTermContainingIgnoreCaseOrDefinitionContainingIgnoreCase(search, search)
                : glossaryTermRepository.findAllByOrderByTermAsc();

        return terms.stream().map(this::toGlossaryTermDto).toList();
    }

    private GlossaryTermDto toGlossaryTermDto(GlossaryTerm term) {
        return GlossaryTermDto.builder()
                .id(term.getId())
                .term(term.getTerm())
                .definition(term.getDefinition())
                .category(term.getCategory())
                .build();
    }

    // ── Quizzes ───────────────────────────────────────────────────────────────

    public List<QuizDto> getQuizzes() {
        return quizRepository.findAll(Sort.by("id")).stream()
                .map(this::toQuizDto)
                .toList();
    }

    private QuizDto toQuizDto(Quiz quiz) {
        List<QuizQuestionDto> questions = quiz.getQuestions().stream()
                .map(this::toQuizQuestionDto)
                .toList();

        return QuizDto.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .difficulty(toFrontendDifficulty(quiz.getDifficulty()))
                .questions(questions)
                .totalQuestions(questions.size())
                .build();
    }

    private QuizQuestionDto toQuizQuestionDto(QuizQuestion question) {
        return QuizQuestionDto.builder()
                .id(question.getId())
                .question(question.getQuestion())
                .options(question.getOptions())
                .correctAnswer(question.getCorrectAnswer())
                .explanation(question.getExplanation())
                .build();
    }

    private String toFrontendDifficulty(Quiz.Difficulty difficulty) {
        return switch (difficulty) {
            case BEGINNER -> "Beginner";
            case INTERMEDIATE -> "Intermediate";
            case ADVANCED -> "Advanced";
        };
    }

    // ── Simulations ───────────────────────────────────────────────────────────

    public List<SimulationScenarioDto> getSimulationScenarios() {
        return articleRepository.findSimulationArticles().stream()
                .map(this::toSimulationScenarioDto)
                .toList();
    }

    public SimulationSubmitResultDto submitSimulationPrediction(Long articleId, String userPrediction) {
        Article article = articleRepository.findByIdWithRelations(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found: " + articleId));

        List<Prediction> predictions = predictionRepository.findByArticleId(articleId);
        String ourPrediction = buildAnalyticalExplanation(article, predictions);

        int score = computeSimilarityScore(userPrediction, ourPrediction);
        String feedback = generateFeedback(score, article.getMentionedSectors());

        return SimulationSubmitResultDto.builder()
                .similarityScore(score)
                .feedback(feedback)
                .ourPrediction(ourPrediction)
                .build();
    }

    private SimulationScenarioDto toSimulationScenarioDto(Article article) {
        String sector = article.getMentionedSectors().stream()
                .findFirst()
                .map(EconomySector::getName)
                .orElse("General");

        String date = article.getPublishedAt() != null
                ? article.getPublishedAt().toLocalDate().toString()
                : "";

        String period = article.getPublishedAt() != null
                ? derivePeriod(article.getPublishedAt())
                : "";

        String difficulty = deriveDifficulty(article.getMentionedSectors().size());

        return SimulationScenarioDto.builder()
                .id(article.getId())
                .title(article.getTitle())
                .date(date)
                .period(period)
                .newsHeadline(article.getTitle())
                .newsContent(article.getContent())
                .context(article.getSummary())
                .sector(sector)
                .difficulty(difficulty)
                .build();
    }

    private String derivePeriod(LocalDateTime dateTime) {
        int month = dateTime.getMonthValue();
        int year = dateTime.getYear();
        String quarter = month <= 3 ? "Q1" : month <= 6 ? "Q2" : month <= 9 ? "Q3" : "Q4";
        return quarter + " " + year;
    }

    private String deriveDifficulty(int sectorCount) {
        if (sectorCount <= 1) return "Beginner";
        if (sectorCount <= 2) return "Intermediate";
        return "Advanced";
    }

    private String buildAnalyticalExplanation(Article article, List<Prediction> predictions) {
        String rationales = predictions.stream()
                .filter(p -> p.getRationale() != null && !p.getRationale().isBlank())
                .map(Prediction::getRationale)
                .collect(Collectors.joining(" "));

        if (!rationales.isBlank()) return rationales;
        return article.getSummary() != null ? article.getSummary() : "";
    }

    private int computeSimilarityScore(String userPrediction, String ourPrediction) {
        if (ourPrediction == null || ourPrediction.isBlank()) return 50;

        // Try semantic scoring via LLM first
        int llmScore = ollamaClient.scoreSimilarity(userPrediction, ourPrediction);
        if (llmScore >= 0) {
            return llmScore;
        }

        // Fallback: keyword-overlap scoring when Ollama is unavailable
        String pred = userPrediction.toLowerCase();
        String actual = ourPrediction.toLowerCase();

        Set<String> keywords = Arrays.stream(actual.split("\\s+"))
                .map(w -> w.replaceAll("[^a-z]", ""))
                .filter(w -> w.length() >= 4)
                .collect(Collectors.toSet());

        if (keywords.isEmpty()) return 50;

        long matches = keywords.stream().filter(pred::contains).count();
        double ratio = (double) matches / keywords.size();

        int lengthBonus = Math.min((int) (userPrediction.length() / 20.0), 10);
        int rawScore = (int) Math.round(ratio * 80) + lengthBonus + 15;
        return Math.min(92, Math.max(25, rawScore));
    }

    private String generateFeedback(int score, Set<EconomySector> sectors) {
        String sectorName = sectors.stream()
                .findFirst()
                .map(EconomySector::getName)
                .orElse("this");

        if (score >= 70) {
            return "Your prediction closely aligns with our analysis — you correctly identified the key market drivers and anticipated the directional movement.";
        } else if (score >= 40) {
            return "Your prediction captured some key trends but missed certain aspects of the analysis; consider how macro factors interact with " + sectorName + " sector dynamics.";
        } else {
            return "Your prediction diverged from our analysis — review how this type of news typically impacts the " + sectorName + " sector and consider multiple stakeholder perspectives.";
        }
    }
}
