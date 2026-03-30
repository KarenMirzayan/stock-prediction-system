package kz.kbtu.webapi.controller;

import kz.kbtu.webapi.dto.MarketSentimentDto;
import kz.kbtu.webapi.service.MarketSentimentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market-sentiment")
@RequiredArgsConstructor
public class MarketSentimentController {

    private final MarketSentimentService marketSentimentService;

    @GetMapping
    public ResponseEntity<MarketSentimentDto> getMarketSentiment() {
        return ResponseEntity.ok(marketSentimentService.getMarketSentiment());
    }
}
