package kz.kbtu.webapi.service;

import kz.kbtu.common.entity.GlossaryTerm;
import kz.kbtu.common.entity.Prediction;
import kz.kbtu.webapi.dto.GlossaryTermDto;
import kz.kbtu.webapi.dto.admin.*;
import kz.kbtu.webapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final ArticleRepository articleRepository;
    private final OllamaEducationClient ollamaClient;
    private final PredictionRepository predictionRepository;
    private final CompanyRepository companyRepository;
    private final GlossaryTermRepository glossaryTermRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    // ── Article ──

    @Transactional
    public void updateArticle(Long id, UpdateArticleRequest request) {
        var article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));
        if (request.getSummary() != null) {
            article.setSummary(request.getSummary());
        }
        if (request.getContent() != null) {
            article.setContent(request.getContent());
        }
        articleRepository.save(article);
    }

    @Transactional
    public void deleteArticle(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new IllegalArgumentException("Article not found");
        }
        articleRepository.deleteById(id);
    }

    // ── Prediction ──

    @Transactional
    public void updatePrediction(Long id, UpdatePredictionRequest request) {
        var prediction = predictionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prediction not found"));
        if (request.getDirection() != null) {
            prediction.setDirection(Prediction.Direction.valueOf(request.getDirection().toUpperCase()));
        }
        if (request.getTimeHorizon() != null) {
            prediction.setTimeHorizon(Prediction.TimeHorizon.valueOf(request.getTimeHorizon().toUpperCase()));
        }
        if (request.getConfidence() != null) {
            prediction.setConfidence(request.getConfidence());
        }
        if (request.getRationale() != null) {
            prediction.setRationale(request.getRationale());
        }
        if (request.getEvidence() != null) {
            prediction.getEvidence().clear();
            prediction.getEvidence().addAll(request.getEvidence());
        }
        predictionRepository.save(prediction);
    }

    @Transactional
    public void deletePrediction(Long id) {
        if (!predictionRepository.existsById(id)) {
            throw new IllegalArgumentException("Prediction not found");
        }
        predictionRepository.deleteById(id);
    }

    // ── Company ──

    @Transactional
    public void updateCompany(Long id, UpdateCompanyRequest request) {
        var company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        if (request.getDescription() != null) {
            company.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            company.setLogoUrl(request.getLogoUrl());
        }
        companyRepository.save(company);
    }

    @Transactional
    public void deleteCompany(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new IllegalArgumentException("Company not found");
        }
        companyRepository.deleteById(id);
    }

    // ── Glossary ──

    @Transactional
    public void updateGlossaryTerm(Long id, UpdateGlossaryTermRequest request) {
        var term = glossaryTermRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Glossary term not found"));
        term.setDefinition(request.getDefinition());
        glossaryTermRepository.save(term);
    }

    @Transactional
    public void deleteGlossaryTerm(Long id) {
        if (!glossaryTermRepository.existsById(id)) {
            throw new IllegalArgumentException("Glossary term not found");
        }
        glossaryTermRepository.deleteById(id);
    }

    @Transactional
    public GlossaryTermDto createGlossaryTerm(CreateGlossaryTermRequest request) {
        String definition = request.getDefinition();
        if (definition == null || definition.isBlank()) {
            definition = ollamaClient.generateDefinition(request.getTerm(), request.getCategory());
            if (definition == null) {
                throw new IllegalStateException("Could not generate definition — Ollama is unavailable");
            }
        }
        var term = GlossaryTerm.builder()
                .term(request.getTerm())
                .definition(definition)
                .category(request.getCategory())
                .build();
        term = glossaryTermRepository.save(term);
        return GlossaryTermDto.builder()
                .id(term.getId())
                .term(term.getTerm())
                .definition(term.getDefinition())
                .category(term.getCategory())
                .build();
    }

    public List<GlossaryTermDto> generateGlossaryTerms(GenerateGlossaryRequest request) {
        List<GlossaryTermDto> results = new ArrayList<>();
        for (var entry : request.getTerms()) {
            try {
                var createReq = new CreateGlossaryTermRequest();
                createReq.setTerm(entry.getTerm());
                createReq.setCategory(entry.getCategory());
                results.add(createGlossaryTerm(createReq));
            } catch (Exception e) {
                log.warn("Failed to generate glossary term '{}': {}", entry.getTerm(), e.getMessage());
            }
        }
        return results;
    }

    // ── Quiz Question ──

    @Transactional
    public void updateQuizQuestion(Long id, UpdateQuizQuestionRequest request) {
        var question = quizQuestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quiz question not found"));
        question.setQuestion(request.getQuestion());
        question.getOptions().clear();
        question.getOptions().addAll(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());
        if (request.getExplanation() != null) {
            question.setExplanation(request.getExplanation());
        }
        quizQuestionRepository.save(question);
    }

    @Transactional
    public void deleteQuiz(Long id) {
        if (!quizRepository.existsById(id)) {
            throw new IllegalArgumentException("Quiz not found");
        }
        quizRepository.deleteById(id);
    }
}
