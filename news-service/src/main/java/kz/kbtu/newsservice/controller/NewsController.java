// Add to NewsController.java
package kz.kbtu.newsservice.controller;

import kz.kbtu.newsservice.service.NewsProcessingService;
import kz.kbtu.newsservice.service.OllamaAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final NewsProcessingService newsProcessingService;
    private final OllamaAnalysisService ollamaService;

    @PostMapping("/scrape")
    public ResponseEntity<String> scrapeNews() {
        log.info("Manual scrape triggered via API");

        String cnbcTechFeed = "https://www.cnbc.com/id/19854910/device/rss/rss.html";

        new Thread(() -> {
            newsProcessingService.processRssFeed(cnbcTechFeed);
        }).start();

        return ResponseEntity.ok("News scraping started. Check logs and 'scraped-articles' folder.");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("News Service is running!");
    }
}