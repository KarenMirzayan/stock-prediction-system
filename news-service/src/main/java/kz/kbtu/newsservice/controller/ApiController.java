package kz.kbtu.newsservice.controller;

import kz.kbtu.newsservice.dto.response.*;
import kz.kbtu.newsservice.service.ApiDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final ApiDataService apiDataService;

    @GetMapping("/articles")
    public ResponseEntity<List<NewsItemDto>> getArticles(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String sentiment,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String company) {
        return ResponseEntity.ok(apiDataService.getArticles(limit, sentiment, sector, company));
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<NewsDetailDto> getArticleDetail(@PathVariable Long id) {
        return ResponseEntity.ok(apiDataService.getArticleDetail(id));
    }

    @GetMapping("/sectors/heatmap")
    public ResponseEntity<List<HeatmapSectorDto>> getHeatmapData() {
        return ResponseEntity.ok(apiDataService.getHeatmapData());
    }

    @GetMapping("/sectors/summary")
    public ResponseEntity<List<SectorDataDto>> getSectorsSummary() {
        return ResponseEntity.ok(apiDataService.getSectorsSummary());
    }

    @GetMapping("/forecasts/stats")
    public ResponseEntity<ForecastStatsDto> getForecastStats() {
        return ResponseEntity.ok(apiDataService.getForecastStats());
    }

    @GetMapping("/forecasts/history")
    public ResponseEntity<List<ForecastHistoryItemDto>> getForecastHistory(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(apiDataService.getForecastHistory(limit));
    }

    @GetMapping("/filters/companies")
    public ResponseEntity<List<String>> getFilterCompanies() {
        return ResponseEntity.ok(apiDataService.getFilterCompanies());
    }

    @GetMapping("/filters/sectors")
    public ResponseEntity<List<String>> getFilterSectors() {
        return ResponseEntity.ok(apiDataService.getFilterSectors());
    }
}
