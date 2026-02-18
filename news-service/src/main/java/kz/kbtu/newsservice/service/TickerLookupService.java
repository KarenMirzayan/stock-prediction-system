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

    // Words stripped from both sides during name comparison (legal/structural, not brand identity)
    private static final Set<String> BUSINESS_SUFFIXES = Set.of(
            "inc", "corp", "corporation", "ltd", "limited", "co", "llc", "lp", "plc",
            "sa", "ag", "nv", "se", "ab", "gmbh", "bv", "sas", "spa", "oy",
            "holding", "holdings", "group", "international", "global",
            "etp", "etf", "token", "usd", "eur", "sek", "com", "the"
    );

    // Extra words in the result name (beyond the query) that are still acceptable
    // These describe corporate structure/domain but don't identify a different company
    private static final Set<String> ALLOWED_NAME_EXTENSIONS = Set.of(
            "platforms", "technologies", "technology", "tech",
            "solutions", "services", "systems", "industries",
            "digital", "labs", "ventures", "capital", "financial",
            "bancorp", "bancshares", "semiconductor", "semiconductors"
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
                if (!isMajorExchange(exchange)) {
                    continue;
                }

                String ticker = item.path("symbol").asText();
                String name = item.path("instrument_name").asText();
                String country = item.path("country").asText();

                if (!isNameSimilar(companyName, name)) {
                    log.info("Skipping '{}' ({}) — name too different from query '{}'", name, ticker, companyName);
                    continue;
                }

                log.info("Resolved '{}' → {} on {} ({})", companyName, ticker, exchange, name);
                return new TickerResult(ticker, exchange, name, country);
            }

            log.info("No verified public company match found for '{}'", companyName);
            return null;

        } catch (Exception e) {
            log.error("Failed to parse Twelve Data response for '{}'", companyName, e);
            return null;
        }
    }

    /**
     * Checks whether a Twelve Data instrument name is close enough to the queried company name.
     *
     * Both names are normalized (lowercased, punctuation removed, business suffixes stripped).
     * The normalized result must start with the normalized query words, and any extra words
     * in the result must be in the allowed generic extensions list.
     *
     * Examples:
     *   "Apple"   → "Apple Inc"              → normalize both → "apple" == "apple"         → true
     *   "Meta"    → "Meta Platforms Inc"      → "meta platforms" starts with "meta",
     *                                            extra "platforms" in allowed list          → true
     *   "Render"  → "Render Cube S.A."        → "render cube" starts with "render",
     *                                            extra "cube" NOT in allowed list           → false
     *   "Render"  → "21Shares Render ETP"     → "21shares render" doesn't start with
     *                                            "render"                                   → false
     */
    boolean isNameSimilar(String query, String resultName) {
        String normQuery = normalizeName(query);
        String normResult = normalizeName(resultName);

        if (normQuery.isEmpty() || normResult.isEmpty()) return false;
        if (normResult.equals(normQuery)) return true;

        String[] queryWords = normQuery.split("\\s+");
        String[] resultWords = normResult.split("\\s+");

        if (resultWords.length < queryWords.length) return false;

        // Result must start with the exact query words
        for (int i = 0; i < queryWords.length; i++) {
            if (!resultWords[i].equals(queryWords[i])) return false;
        }

        // Any extra words in the result must be in the allowed generic extensions
        for (int i = queryWords.length; i < resultWords.length; i++) {
            if (!ALLOWED_NAME_EXTENSIONS.contains(resultWords[i])) {
                log.info("Name mismatch: '{}' vs '{}' — extra word '{}' not allowed",
                        query, resultName, resultWords[i]);
                return false;
            }
        }

        return true;
    }

    /**
     * Normalizes a company name for comparison:
     * lowercases, removes punctuation, strips business/legal suffixes.
     */
    private String normalizeName(String name) {
        if (name == null) return "";
        String lower = name.toLowerCase();
        lower = lower.replaceAll("\\.com\\b", ""); // strip .com domains before splitting
        lower = lower.replaceAll("[^a-z0-9 ]", " "); // remove punctuation
        String[] words = lower.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty() && !BUSINESS_SUFFIXES.contains(word)) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(word);
            }
        }
        return sb.toString().trim();
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
