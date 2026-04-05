package kz.kbtu.webapi.controller;

import kz.kbtu.webapi.dto.ForecastHistoryItemDto;
import kz.kbtu.webapi.dto.ForecastStatsDto;
import kz.kbtu.webapi.service.ForecastAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastAnalyticsService forecastAnalyticsService;

    @GetMapping("/stats")
    public ResponseEntity<ForecastStatsDto> getStats() {
        return ResponseEntity.ok(forecastAnalyticsService.getStats());
    }

    @GetMapping("/history")
    public ResponseEntity<List<ForecastHistoryItemDto>> getHistory() {
        return ResponseEntity.ok(forecastAnalyticsService.getHistory());
    }
}
