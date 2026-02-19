package kz.kbtu.webapi.service;

import kz.kbtu.common.entity.GlossaryTerm;
import kz.kbtu.common.entity.Quiz;
import kz.kbtu.common.entity.QuizQuestion;
import kz.kbtu.webapi.dto.GlossaryTermDto;
import kz.kbtu.webapi.dto.QuizDto;
import kz.kbtu.webapi.dto.QuizQuestionDto;
import kz.kbtu.webapi.repository.GlossaryTermRepository;
import kz.kbtu.webapi.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EducationService {

    private final GlossaryTermRepository glossaryTermRepository;
    private final QuizRepository quizRepository;

    public List<GlossaryTermDto> getGlossaryTerms(String search) {
        List<GlossaryTerm> terms = (search != null && !search.isBlank())
                ? glossaryTermRepository.findByTermContainingIgnoreCaseOrDefinitionContainingIgnoreCase(search, search)
                : glossaryTermRepository.findAllByOrderByTermAsc();

        return terms.stream().map(this::toGlossaryTermDto).toList();
    }

    public List<QuizDto> getQuizzes() {
        return quizRepository.findAll(Sort.by("id")).stream()
                .map(this::toQuizDto)
                .toList();
    }

    private GlossaryTermDto toGlossaryTermDto(GlossaryTerm term) {
        return GlossaryTermDto.builder()
                .id(term.getId())
                .term(term.getTerm())
                .definition(term.getDefinition())
                .category(term.getCategory())
                .build();
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
}
