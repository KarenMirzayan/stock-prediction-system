package kz.kbtu.newsservice.scheduler;

import kz.kbtu.common.entity.Company;
import kz.kbtu.common.entity.RedditBuzz;
import kz.kbtu.newsservice.repository.CompanyRepository;
import kz.kbtu.newsservice.repository.RedditBuzzRepository;
import kz.kbtu.newsservice.service.ApeWisdomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedditBuzzScheduler {

    private final ApeWisdomService apeWisdomService;
    private final RedditBuzzRepository redditBuzzRepository;
    private final CompanyRepository companyRepository;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        refreshBuzz();
    }

    @Scheduled(fixedRate = 7_200_000) // every 2 hours
    public void scheduledRefresh() {
        refreshBuzz();
    }

    @Transactional
    public void refreshBuzz() {
        if (!running.compareAndSet(false, true)) {
            log.info("Reddit buzz refresh already in progress — skipping");
            return;
        }
        try {
            log.info("══════ Reddit Buzz Refresh Started ══════");
            long startTime = System.currentTimeMillis();

            List<ApeWisdomService.BuzzItem> items = apeWisdomService.fetchAllPages();

            if (items.isEmpty()) {
                log.warn("ApeWisdom returned no data — keeping existing buzz data");
                return;
            }

            // Build ticker -> Company lookup (case-insensitive)
            log.info("Loading companies from DB for ticker matching...");
            Map<String, Company> companyByTicker = companyRepository.findAll().stream()
                    .collect(Collectors.toMap(
                            c -> c.getTicker().toUpperCase(),
                            Function.identity(),
                            (a, b) -> a
                    ));
            log.info("Loaded {} companies from DB", companyByTicker.size());

            // Only keep tickers that match companies in our DB
            List<RedditBuzz> buzzEntities = items.stream()
                    .filter(item -> companyByTicker.containsKey(item.ticker().toUpperCase()))
                    .map(item -> RedditBuzz.builder()
                            .ticker(item.ticker())
                            .name(item.name())
                            .rank(item.rank())
                            .mentions(item.mentions())
                            .upvotes(item.upvotes())
                            .rank24hAgo(item.rank24hAgo())
                            .mentions24hAgo(item.mentions24hAgo())
                            .company(companyByTicker.get(item.ticker().toUpperCase()))
                            .build())
                    .toList();

            log.info("Matched {}/{} tickers to DB companies", buzzEntities.size(), items.size());

            log.info("Saving to DB: deleting old rows...");
            redditBuzzRepository.deleteAllInBatch();
            log.info("Inserting {} rows...", buzzEntities.size());
            redditBuzzRepository.saveAll(buzzEntities);

            long elapsed = System.currentTimeMillis() - startTime;

            // Log all matched companies by rank
            buzzEntities.stream()
                    .sorted((a, b) -> Integer.compare(a.getRank(), b.getRank()))
                    .forEach(b -> log.info("  #{} {} — {} mentions, {} upvotes",
                            b.getRank(), b.getTicker(), b.getMentions(), b.getUpvotes()));

            log.info("══════ Reddit Buzz Refresh Complete ══════");
            log.info("  Fetched: {} tickers, Saved: {}, Duration: {}s",
                    items.size(), buzzEntities.size(), elapsed / 1000);
        } catch (Exception e) {
            log.error("Reddit buzz refresh failed: {}", e.getMessage(), e);
        } finally {
            running.set(false);
        }
    }
}
