package kz.kbtu.webapi.controller;

import kz.kbtu.webapi.dto.NewsDetailDto;
import kz.kbtu.webapi.dto.NewsPageDto;
import kz.kbtu.webapi.service.NewsApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsApiController {

    private final NewsApiService newsApiService;

    @GetMapping
    public ResponseEntity<NewsPageDto> getLatestNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(newsApiService.getLatestNews(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsDetailDto> getNewsDetail(@PathVariable Long id) {
        return newsApiService.getNewsDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/company/{ticker}")
    public ResponseEntity<NewsPageDto> getNewsByCompany(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(newsApiService.getNewsByCompany(ticker, page, size));
    }

    @GetMapping("/sector/{sectorCode}")
    public ResponseEntity<NewsPageDto> getNewsBySector(
            @PathVariable String sectorCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(newsApiService.getNewsBySector(sectorCode, page, size));
    }

    @GetMapping("/sentiment/{sentiment}")
    public ResponseEntity<NewsPageDto> getNewsBySentiment(
            @PathVariable String sentiment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(newsApiService.getNewsBySentiment(sentiment, page, size));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Web API is running!");
    }
}
