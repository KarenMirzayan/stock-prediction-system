package kz.kbtu.newsservice.service;

import kz.kbtu.common.dto.ArticleAnalysisDto;
import kz.kbtu.common.dto.MarketEventDto;
import kz.kbtu.common.dto.RssArticleDto;
import kz.kbtu.common.entity.Article;
import kz.kbtu.common.entity.Company;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsProcessingService {

    private final RssFeedService rssFeedService;
    private final ArticleScraperService scraperService;
    private final FileStorageService fileStorageService;
    private final OllamaAnalysisService ollamaService;
    private final ArticleService articleService;
    private final MarketEventService marketEventService;

    public void processRssFeed(String feedUrl) {
        log.info("Starting RSS feed processing with LLM analysis and database persistence...");

        // Check if Ollama is available
        if (!ollamaService.isAvailable()) {
            log.error("Ollama is not available! Start it with: ollama serve");
            return;
        }

        List<RssArticleDto> articles = rssFeedService.fetchFeed(feedUrl);

        if (articles.isEmpty()) {
            log.warn("No articles found in RSS feed");
            return;
        }

        // Batch check existing articles in one query
        Set<String> existingIds = articleService.findExistingCnbcIds(
                articles.stream().map(RssArticleDto::getExternalId).collect(Collectors.toList())
        );

        log.info("Processing {} articles ({} already in DB)...", articles.size(), existingIds.size());

        int successCount = 0;
        int skippedCount = existingIds.size();

        for (int i = 0; i < articles.size(); i++) {
            RssArticleDto rssArticle = articles.get(i);

            if (existingIds.contains(rssArticle.getExternalId())) {
                log.info("[{}/{}] Already processed, skipping: {}", i + 1, articles.size(), rssArticle.getTitle());
                continue;
            }

            log.info("[{}/{}] Processing: {}", i + 1, articles.size(), rssArticle.getTitle());

            try {

                // Step 1: Create article record from RSS
                Article article = articleService.createArticleFromRss(
                        rssArticle.getExternalId(),
                        rssArticle.getTitle(),
                        rssArticle.getUrl(),
                        rssArticle.getDescription(),
                        rssArticle.getPublishedAt()
                );

                if (article == null) {
                    log.warn("Failed to create article record for: {}", rssArticle.getTitle());
                    continue;
                }

                // Step 2: Scrape article content
                String content = scraperService.scrapeArticle(rssArticle.getUrl());

                if (content != null && !content.isEmpty()) {
                    // Update article with scraped content
                    article = articleService.updateWithScrapedContent(article.getId(), content);

                    // Step 3: Analyze with LLM
                    ArticleAnalysisDto analysis = ollamaService.analyzeArticle(
                            rssArticle.getTitle(),
                            content
                    );

                    // Step 4: Process analysis and persist to database
                    article = articleService.processAnalysis(
                            article.getId(),
                            analysis,
                            ollamaService.getModelName()
                    );

                    // Step 5: Extract and save calendar events
                    Map<String, String> tickerMap = article.getMentionedCompanies().stream()
                            .collect(Collectors.toMap(
                                    Company::getName,
                                    Company::getTicker,
                                    (a, b) -> a
                            ));
                    LocalDate articleDate = rssArticle.getPublishedAt() != null
                            ? rssArticle.getPublishedAt().toLocalDate()
                            : LocalDate.now();
                    List<MarketEventDto> events = ollamaService.extractEvents(
                            rssArticle.getTitle(), content, tickerMap, articleDate);
                    if (!events.isEmpty()) {
                        marketEventService.saveEvents(events, article);
                    }

                    // Step 7: Also save to file for backup/review
                    fileStorageService.saveArticleWithAnalysis(
                            rssArticle.getTitle(),
                            rssArticle.getUrl(),
                            content,
                            convertAnalysisToMap(analysis)
                    );

                    log.info("Successfully processed article: {} (ID: {}, Predictions: {})",
                            article.getTitle(),
                            article.getId(),
                            article.getPredictions().size());

                    successCount++;
                } else {
                    log.warn("Skipping article (no content): {}", rssArticle.getTitle());
                }

            } catch (Exception e) {
                log.error("Failed to process article: {}", rssArticle.getTitle(), e);
            }

            // Rate limiting
            if (i < articles.size() - 1) {
                try {
                    Thread.sleep(3000); // 3 seconds between articles
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        fileStorageService.saveAllArticlesSummary(articles.size(), successCount);

        log.info("RSS feed processing completed. Success: {}/{}, Skipped: {}",
                successCount, articles.size(), skippedCount);
    }

    /**
     * Process a single article by URL (useful for testing or manual processing)
     */
    public Article processSingleArticle(String cnbcId, String title, String url, String description) {
        if (!ollamaService.isAvailable()) {
            throw new RuntimeException("Ollama is not available");
        }

        // Create article record
        Article article = articleService.createArticleFromRss(cnbcId, title, url, description, null);
        if (article == null) {
            throw new RuntimeException("Failed to create article record");
        }

        // Scrape content
        String content = scraperService.scrapeArticle(url);
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Failed to scrape article content");
        }

        // Update with scraped content
        article = articleService.updateWithScrapedContent(article.getId(), content);

        // Analyze with LLM
        ArticleAnalysisDto analysis = ollamaService.analyzeArticle(title, content);

        // Process and persist
        return articleService.processAnalysis(article.getId(), analysis, ollamaService.getModelName());
    }

    private java.util.Map<String, Object> convertAnalysisToMap(ArticleAnalysisDto analysis) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("summary", analysis.getSummary());
        map.put("sentiment", analysis.getSentiment());
        map.put("companies", analysis.getCompanies());
        map.put("countries", analysis.getCountries());
        map.put("sectors", analysis.getSectors());

        java.util.List<java.util.Map<String, Object>> predictions = new java.util.ArrayList<>();
        for (ArticleAnalysisDto.PredictionDto pred : analysis.getPredictions()) {
            java.util.Map<String, Object> predMap = new java.util.HashMap<>();
            predMap.put("scope", pred.getScope());
            predMap.put("targets", pred.getTargets());
            predMap.put("direction", pred.getDirection());
            predMap.put("timeHorizon", pred.getTimeHorizon());
            predMap.put("confidence", pred.getConfidence());
            predMap.put("rationale", pred.getRationale());
            predMap.put("evidence", pred.getEvidence());
            predictions.add(predMap);
        }
        map.put("predictions", predictions);

        return map;
    }
}