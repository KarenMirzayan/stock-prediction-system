package kz.kbtu.newsservice.scheduler;

import kz.kbtu.newsservice.service.NewsProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.news-scrape.enabled", havingValue = "true")
public class NewsScrapeScheduler {

    private static final String CNBC_TECH_FEED = "https://www.cnbc.com/id/19854910/device/rss/rss.html";

    private final NewsProcessingService newsProcessingService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        scrapeNews();
    }

    @Scheduled(fixedRate = 1_800_000) // every 30 minutes
    public void scrapeNews() {
        if (!running.compareAndSet(false, true)) {
            log.info("News scrape already in progress — skipping");
            return;
        }
        try {
            log.info("Scheduled news scrape starting...");
            newsProcessingService.processRssFeed(CNBC_TECH_FEED);
        } catch (Exception e) {
            log.error("Scheduled news scrape failed: {}", e.getMessage(), e);
        } finally {
            running.set(false);
        }
    }
}
