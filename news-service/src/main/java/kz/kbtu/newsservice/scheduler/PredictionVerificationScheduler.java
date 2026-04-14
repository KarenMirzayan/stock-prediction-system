package kz.kbtu.newsservice.scheduler;

import kz.kbtu.common.entity.Company;
import kz.kbtu.common.entity.Prediction;
import kz.kbtu.common.entity.Prediction.Direction;
import kz.kbtu.common.entity.Prediction.TimeHorizon;
import kz.kbtu.newsservice.repository.PredictionRepository;
import kz.kbtu.newsservice.service.StockPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.prediction-verification.enabled", havingValue = "true")
public class PredictionVerificationScheduler {

    private final PredictionRepository predictionRepository;
    private final StockPriceService stockPriceService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private static final int PER_RUN_API_BUDGET = 21; // ~2 min of API calls per 30-min run (800/day shared with TickerLookupService)
    private static final long THROTTLE_MS = 8_000; // ~7.5 calls/min, under 8/min limit
    private static final int STALENESS_DAYS = 180;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        verifyPredictions();
    }

    @Scheduled(initialDelay = 900_000, fixedRate = 1_800_000) // 15 min after startup, then every 30 min
    public void verifyPredictions() {
        if (!running.compareAndSet(false, true)) {
            log.info("Prediction verification already in progress — skipping");
            return;
        }
        try {
            doVerify();
        } finally {
            running.set(false);
        }
    }

    private void doVerify() {
        List<Prediction> candidates = predictionRepository.findUnverifiedPredictions();
        log.info("Found {} unverified predictions to check", candidates.size());

        AtomicInteger apiCalls = new AtomicInteger(0);
        int verified = 0;
        int skipped = 0;
        int stale = 0;

        for (Prediction prediction : candidates) {
            if (apiCalls.get() >= PER_RUN_API_BUDGET) {
                log.warn("Daily API budget ({}) exhausted. {} predictions remain unverified.",
                        PER_RUN_API_BUDGET, candidates.size() - verified - skipped - stale);
                break;
            }

            // Mark stale predictions to stop retrying
            if (prediction.getCreatedAt().plusDays(STALENESS_DAYS).isBefore(LocalDateTime.now())) {
                prediction.setVerifiedAt(LocalDateTime.now());
                predictionRepository.save(prediction);
                stale++;
                continue;
            }

            // Check if prediction has matured based on time horizon
            LocalDateTime verifyAfter = computeVerifyAfter(prediction);
            if (verifyAfter == null || LocalDateTime.now().isBefore(verifyAfter)) {
                skipped++;
                continue;
            }

            LocalDate fromDate = prediction.getCreatedAt().toLocalDate();
            LocalDate toDate = verifyAfter.toLocalDate();

            try {
                Optional<Double> movement = getMovementForPrediction(prediction, fromDate, toDate, apiCalls);
                if (movement.isEmpty()) {
                    skipped++;
                    continue;
                }

                double movementPct = movement.get();
                boolean accurate = determineAccuracy(prediction.getDirection(), movementPct);

                prediction.setActualMovementPct(movementPct);
                prediction.setAccurate(accurate);
                prediction.setVerifiedAt(LocalDateTime.now());
                predictionRepository.save(prediction);
                verified++;

                if (verified % 10 == 0) {
                    log.info("Verification progress: {} verified, {} skipped, {} stale, {} API calls used",
                            verified, skipped, stale, apiCalls.get());
                }

                throttle();

            } catch (StockPriceService.RateLimitException e) {
                log.warn("Rate limited during verification. Stopping run. {} verified so far.", verified);
                break;
            } catch (Exception e) {
                log.warn("Error verifying prediction {}: {}", prediction.getId(), e.getMessage());
                skipped++;
            }
        }

        log.info("Prediction verification complete: {} verified, {} skipped, {} stale, {} API calls",
                verified, skipped, stale, apiCalls.get());
    }

    private Optional<Double> getMovementForPrediction(
            Prediction prediction, LocalDate fromDate, LocalDate toDate, AtomicInteger apiCalls) {

        if (prediction.getScope() == Prediction.PredictionScope.COMPANY) {
            Company company = prediction.getCompany();
            if (company == null || company.getTicker() == null) return Optional.empty();

            apiCalls.incrementAndGet();
            return stockPriceService.getMovementPct(company.getTicker(), fromDate, toDate);
        }

        // MULTI_TICKER: average movement across all companies
        Set<Company> companies = prediction.getCompanies();
        if (companies == null || companies.isEmpty()) return Optional.empty();

        List<Double> movements = new ArrayList<>();
        for (Company company : companies) {
            if (company.getTicker() == null) continue;

            apiCalls.incrementAndGet();
            if (apiCalls.get() >= PER_RUN_API_BUDGET) break;

            Optional<Double> mov = stockPriceService.getMovementPct(company.getTicker(), fromDate, toDate);
            mov.ifPresent(movements::add);
            throttle();
        }

        // Need at least 50% of companies to have data
        if (movements.size() < Math.ceil(companies.size() / 2.0)) {
            return Optional.empty();
        }

        double avg = movements.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return Optional.of(Math.round(avg * 100.0) / 100.0);
    }

    private boolean determineAccuracy(Direction direction, double movementPct) {
        return switch (direction) {
            case BULLISH -> movementPct > 0;
            case BEARISH -> movementPct < 0;
            case NEUTRAL -> Math.abs(movementPct) < 1.0;
            default -> false;
        };
    }

    private LocalDateTime computeVerifyAfter(Prediction prediction) {
        TimeHorizon horizon = prediction.getTimeHorizon();
        if (horizon == null) return null;

        return switch (horizon) {
            case SHORT_TERM -> prediction.getCreatedAt().plusDays(1);
            case MID_TERM -> prediction.getCreatedAt().plusDays(3);
            case LONG_TERM -> prediction.getCreatedAt().plusDays(7);
        };
    }

    private void throttle() {
        try {
            Thread.sleep(THROTTLE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
