package kz.kbtu.newsservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@Slf4j
public class TickerLookupService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private static final Set<String> MAJOR_EXCHANGES = Set.of(
            "NYSE", "NASDAQ", "LSE", "TSE", "HKEX", "SSE", "SZSE",
            "BSE", "NSE", "KRX", "TWSE", "ASX", "TSX", "XETRA",
            "Euronext", "SIX", "JSE", "SGX", "SET", "BM"
    );

    private static final int MAX_RETRIES = 3;

    public TickerLookupService(
            @Value("${twelvedata.base-url:https://api.twelvedata.com}") String baseUrl,
            @Value("${twelvedata.api-key:demo}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "apikey " + apiKey)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public record TickerResult(String ticker, String exchange, String instrumentName, String country) {}

    /**
     * Looks up a company name against Twelve Data symbol_search API.
     * Returns the best match on a major exchange, or null if the company is not publicly traded.
     */
    public TickerResult lookupTicker(String companyName) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String responseBody = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/symbol_search")
                                .queryParam("symbol", companyName)
                                .queryParam("outputsize", 10)
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

                return parseBestMatch(responseBody, companyName);

            } catch (RateLimitException e) {
                long waitSeconds = e.retryAfterSeconds;
                log.warn("Twelve Data rate limited. Waiting {} seconds before retry (attempt {}/{})",
                        waitSeconds, attempt, MAX_RETRIES);
                try {
                    Thread.sleep(waitSeconds * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } catch (Exception e) {
                log.error("Failed to lookup ticker for '{}' (attempt {}/{})", companyName, attempt, MAX_RETRIES, e);
                if (attempt == MAX_RETRIES) return null;
            }
        }
        return null;
    }

    private TickerResult parseBestMatch(String responseBody, String companyName) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");

            if (!data.isArray() || data.isEmpty()) {
                log.info("No results found for company '{}'", companyName);
                return null;
            }

            // First pass: find a match on a major exchange (only common stocks, skip ETPs/ETFs/crypto)
            for (JsonNode item : data) {
                String type = item.path("instrument_type").asText();
                if (!"Common Stock".equals(type)) {
                    continue;
                }

                String exchange = item.path("exchange").asText();
                if (isMajorExchange(exchange)) {
                    String ticker = item.path("symbol").asText();
                    String name = item.path("instrument_name").asText();
                    String country = item.path("country").asText();
                    log.info("Resolved '{}' → {} on {} ({})", companyName, ticker, exchange, name);
                    return new TickerResult(ticker, exchange, name, country);
                }
            }

            log.info("No major exchange listing found for '{}'", companyName);
            return null;

        } catch (Exception e) {
            log.error("Failed to parse Twelve Data response for '{}'", companyName, e);
            return null;
        }
    }

    private boolean isMajorExchange(String exchange) {
        if (exchange == null) return false;
        for (String major : MAJOR_EXCHANGES) {
            if (exchange.toUpperCase().contains(major.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    private long parseRetryAfter(String retryAfterHeader) {
        if (retryAfterHeader != null) {
            try {
                return Long.parseLong(retryAfterHeader);
            } catch (NumberFormatException ignored) {}
        }
        return 60; // Default wait: 60 seconds
    }

    private static class RateLimitException extends RuntimeException {
        final long retryAfterSeconds;

        RateLimitException(long retryAfterSeconds) {
            super("Rate limited, retry after " + retryAfterSeconds + "s");
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }
}
