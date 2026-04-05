package kz.kbtu.newsservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Slf4j
public class StockPriceService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRIES = 3;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public StockPriceService(
            @Value("${twelvedata.base-url:https://api.twelvedata.com}") String baseUrl,
            @Value("${twelvedata.api-key:demo}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "apikey " + apiKey)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Computes the percentage price movement for a ticker between two dates.
     * Brackets dates wider to handle weekends/holidays — finds closest trading days.
     *
     * @return percentage change, e.g. +3.2 or -1.5, or empty if data unavailable
     */
    public Optional<Double> getMovementPct(String ticker, LocalDate fromDate, LocalDate toDate) {
        try {
            // Widen bracket to handle weekends/holidays
            String startDate = fromDate.minusDays(4).format(DATE_FMT);
            String endDate = toDate.plusDays(1).format(DATE_FMT);

            String responseBody = fetchTimeSeries(ticker, startDate, endDate);
            if (responseBody == null) return Optional.empty();

            JsonNode root = objectMapper.readTree(responseBody);

            if (!"ok".equals(root.path("status").asText())) {
                log.warn("Twelve Data returned error for {}: {}", ticker, root.path("message").asText());
                return Optional.empty();
            }

            JsonNode values = root.path("values");
            if (!values.isArray() || values.isEmpty()) {
                log.warn("No price data returned for {} between {} and {}", ticker, fromDate, toDate);
                return Optional.empty();
            }

            // Values are ordered newest-first from Twelve Data
            // Use last entry (oldest, near fromDate) and first entry (newest, near toDate)
            JsonNode newestEntry = values.get(0);
            JsonNode oldestEntry = values.get(values.size() - 1);

            Double fromClose = parseDouble(oldestEntry.path("close").asText());
            Double toClose = parseDouble(newestEntry.path("close").asText());

            log.debug("Twelve Data for {}: oldest={} close={}, newest={} close={}",
                    ticker,
                    oldestEntry.path("datetime").asText(), oldestEntry.path("close").asText(),
                    newestEntry.path("datetime").asText(), newestEntry.path("close").asText());

            if (fromClose == null || toClose == null || fromClose <= 0) {
                log.warn("Invalid close prices for {} (from={}, to={})", ticker, fromClose, toClose);
                return Optional.empty();
            }

            double movementPct = ((toClose - fromClose) / fromClose) * 100.0;
            movementPct = Math.round(movementPct * 100.0) / 100.0; // round to 2 decimals

            log.info("Price movement for {} from {} to {}: {}% (${} → ${})",
                    ticker, fromDate, toDate, movementPct, fromClose, toClose);
            return Optional.of(movementPct);

        } catch (RateLimitException e) {
            throw e; // let caller handle rate limiting
        } catch (Exception e) {
            log.error("Failed to get movement for {}: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    private String fetchTimeSeries(String ticker, String startDate, String endDate) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/time_series")
                                .queryParam("symbol", ticker)
                                .queryParam("interval", "1day")
                                .queryParam("start_date", startDate)
                                .queryParam("end_date", endDate)
                                .build())
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, response -> {
                            if (response.statusCode().value() == 429) {
                                return Mono.error(new RateLimitException(
                                        parseRetryAfter(response.headers().asHttpHeaders().getFirst("Retry-After"))));
                            }
                            return response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new RuntimeException("API error: " + body)));
                        })
                        .bodyToMono(String.class)
                        .block();

            } catch (RateLimitException e) {
                long waitSeconds = e.retryAfterSeconds;
                log.warn("Twelve Data rate limited for {}. Waiting {}s (attempt {}/{})",
                        ticker, waitSeconds, attempt, MAX_RETRIES);
                try {
                    Thread.sleep(waitSeconds * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } catch (Exception e) {
                log.error("Failed to fetch time series for {} (attempt {}/{}): {}",
                        ticker, attempt, MAX_RETRIES, e.getMessage());
                if (attempt == MAX_RETRIES) return null;
            }
        }
        return null;
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private long parseRetryAfter(String retryAfterHeader) {
        if (retryAfterHeader != null) {
            try {
                return Long.parseLong(retryAfterHeader);
            } catch (NumberFormatException ignored) {}
        }
        return 60;
    }

    public static class RateLimitException extends RuntimeException {
        final long retryAfterSeconds;

        RateLimitException(long retryAfterSeconds) {
            super("Rate limited, retry after " + retryAfterSeconds + "s");
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }
}
