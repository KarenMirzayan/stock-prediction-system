package kz.kbtu.newsservice.scheduler;

import kz.kbtu.common.entity.Company;
import kz.kbtu.newsservice.repository.CompanyRepository;
import kz.kbtu.newsservice.service.FinnhubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
@RequiredArgsConstructor
public class QuoteRefreshScheduler {

    private final CompanyRepository companyRepository;
    private final FinnhubService finnhubService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * On startup: backfill shareOutstanding, then refresh quotes so the heatmap has data immediately.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        backfillShareOutstanding();
        refreshQuotes();
    }

    private void backfillShareOutstanding() {
        List<Company> companies = companyRepository.findByShareOutstandingIsNull();
        if (companies.isEmpty()) {
            return;
        }
        log.info("Backfilling shareOutstanding for {} companies", companies.size());
        for (Company company : companies) {
            try {
                FinnhubService.CompanyProfile profile = finnhubService.getProfile(company.getTicker());
                if (profile != null && profile.shareOutstanding() > 0) {
                    company.setShareOutstanding(profile.shareOutstanding());
                    companyRepository.save(company);
                    log.info("Backfilled shareOutstanding for {} = {}", company.getTicker(), profile.shareOutstanding());
                }
                Thread.sleep(1100); // rate limit: 60 calls/min
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Failed to backfill shareOutstanding for {}: {}", company.getTicker(), e.getMessage());
            }
        }
    }

    // 9:30–9:55 ET, every 5 min
    @Scheduled(cron = "0 30-59/5 9 * * MON-FRI", zone = "America/New_York")
    // 10:00–15:55 ET, every 5 min
    @Scheduled(cron = "0 */5 10-15 * * MON-FRI", zone = "America/New_York")
    // 16:05 ET final snapshot
    @Scheduled(cron = "0 5 16 * * MON-FRI", zone = "America/New_York")
    public void refreshQuotes() {
        if (!running.compareAndSet(false, true)) {
            log.info("Quote refresh already in progress — skipping");
            return;
        }
        try {
            List<Company> companies = companyRepository.findByCountryCode("US");
            log.info("Refreshing quotes for {} US companies", companies.size());
            for (Company company : companies) {
                try {
                    FinnhubService.StockQuote quote = finnhubService.getQuote(company.getTicker());
                    if (quote != null) {
                        company.setDailyChangePercent(quote.changePercent());
                        if (company.getShareOutstanding() != null && company.getShareOutstanding() > 0) {
                            company.setMarketCap(quote.currentPrice() * company.getShareOutstanding());
                        }
                        companyRepository.save(company);
                    }
                    Thread.sleep(1100); // rate limit: 60 calls/min
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.warn("Failed to refresh quote for {}: {}", company.getTicker(), e.getMessage());
                }
            }
            log.info("Quote refresh complete");
        } finally {
            running.set(false);
        }
    }
}
