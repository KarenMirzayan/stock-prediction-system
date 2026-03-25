package kz.kbtu.webapi.controller;

import jakarta.validation.Valid;
import kz.kbtu.webapi.dto.GlossaryTermDto;
import kz.kbtu.webapi.dto.admin.*;
import kz.kbtu.webapi.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ── Articles ──

    @PutMapping("/articles/{id}")
    public ResponseEntity<Void> updateArticle(@PathVariable Long id,
                                              @Valid @RequestBody UpdateArticleRequest request) {
        adminService.updateArticle(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        adminService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }

    // ── Predictions ──

    @PutMapping("/predictions/{id}")
    public ResponseEntity<Void> updatePrediction(@PathVariable Long id,
                                                 @Valid @RequestBody UpdatePredictionRequest request) {
        adminService.updatePrediction(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/predictions/{id}")
    public ResponseEntity<Void> deletePrediction(@PathVariable Long id) {
        adminService.deletePrediction(id);
        return ResponseEntity.noContent().build();
    }

    // ── Companies ──

    @PutMapping("/companies/{id}")
    public ResponseEntity<Void> updateCompany(@PathVariable Long id,
                                              @Valid @RequestBody UpdateCompanyRequest request) {
        adminService.updateCompany(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/companies/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        adminService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

    // ── Glossary ──

    @PostMapping("/glossary")
    public ResponseEntity<GlossaryTermDto> createGlossaryTerm(
            @Valid @RequestBody CreateGlossaryTermRequest request) {
        return ResponseEntity.ok(adminService.createGlossaryTerm(request));
    }

    @PostMapping("/glossary/generate")
    public ResponseEntity<List<GlossaryTermDto>> generateGlossaryTerms(
            @Valid @RequestBody GenerateGlossaryRequest request) {
        return ResponseEntity.ok(adminService.generateGlossaryTerms(request));
    }

    @PutMapping("/glossary/{id}")
    public ResponseEntity<Void> updateGlossaryTerm(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateGlossaryTermRequest request) {
        adminService.updateGlossaryTerm(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/glossary/{id}")
    public ResponseEntity<Void> deleteGlossaryTerm(@PathVariable Long id) {
        adminService.deleteGlossaryTerm(id);
        return ResponseEntity.noContent().build();
    }

    // ── Quizzes ──

    @PutMapping("/quizzes/questions/{id}")
    public ResponseEntity<Void> updateQuizQuestion(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateQuizQuestionRequest request) {
        adminService.updateQuizQuestion(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/quizzes/{id}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long id) {
        adminService.deleteQuiz(id);
        return ResponseEntity.noContent().build();
    }
}
