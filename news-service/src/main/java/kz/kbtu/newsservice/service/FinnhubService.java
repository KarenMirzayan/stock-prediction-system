package kz.kbtu.newsservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class FinnhubService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public FinnhubService(@Value("${finnhub.api-key:demo}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://finnhub.io/api/v1")
                .defaultHeader("X-Finnhub-Token", apiKey)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public record CompanyProfile(
            String finnhubIndustry,
            String logoUrl,
            String webUrl,
            double marketCap,
            String ipoDate,
            String country,
            String currency
    ) {}

    /**
     * Fetches company profile from Finnhub /stock/profile2 endpoint.
     * Returns null if the ticker is not found or the API call fails.
     */
    public CompanyProfile getProfile(String ticker) {
        try {
            String responseBody = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stock/profile2")
                            .queryParam("symbol", ticker)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody == null || responseBody.isBlank() || "{}".equals(responseBody.trim())) {
                log.info("Finnhub returned empty profile for ticker '{}'", ticker);
                return null;
            }

            JsonNode root = objectMapper.readTree(responseBody);

            if (!root.has("ticker") || root.path("ticker").asText().isBlank()) {
                log.info("Finnhub returned no data for ticker '{}'", ticker);
                return null;
            }

            return new CompanyProfile(
                    root.path("finnhubIndustry").asText(null),
                    root.path("logo").asText(null),
                    root.path("weburl").asText(null),
                    root.path("marketCapitalization").asDouble(0),
                    root.path("ipo").asText(null),
                    root.path("country").asText(null),
                    root.path("currency").asText(null)
            );

        } catch (Exception e) {
            log.warn("Failed to fetch Finnhub profile for '{}': {}", ticker, e.getMessage());
            return null;
        }
    }
}
