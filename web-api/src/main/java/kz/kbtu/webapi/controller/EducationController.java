package kz.kbtu.webapi.controller;

import kz.kbtu.webapi.dto.GlossaryTermDto;
import kz.kbtu.webapi.dto.QuizDto;
import kz.kbtu.webapi.service.EducationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/education")
@RequiredArgsConstructor
public class EducationController {

    private final EducationService educationService;

    @GetMapping("/glossary")
    public ResponseEntity<List<GlossaryTermDto>> getGlossaryTerms(
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(educationService.getGlossaryTerms(search));
    }

    @GetMapping("/quizzes")
    public ResponseEntity<List<QuizDto>> getQuizzes() {
        return ResponseEntity.ok(educationService.getQuizzes());
    }
}
