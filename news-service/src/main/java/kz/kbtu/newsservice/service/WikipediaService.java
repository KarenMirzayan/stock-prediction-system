package kz.kbtu.newsservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class WikipediaService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public WikipediaService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://en.wikipedia.org")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Looks up a company on Wikipedia and returns the summary extract.
     * Uses opensearch to find the page title, then fetches the summary.
     * Returns null if no Wikipedia page is found.
     */
    public String getCompanyDescription(String companyName) {
        try {
            // Step 1: Search for the Wikipedia page title
            String pageTitle = searchPageTitle(companyName);
            if (pageTitle == null) {
                log.info("No Wikipedia page found for '{}'", companyName);
                return null;
            }

            // Step 2: Fetch the summary extract
            return fetchSummary(pageTitle);

        } catch (Exception e) {
            log.warn("Failed to fetch Wikipedia description for '{}': {}", companyName, e.getMessage());
            return null;
        }
    }

    private String searchPageTitle(String query) throws Exception {
        String responseBody = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/w/api.php")
                        .queryParam("action", "opensearch")
                        .queryParam("search", query)
                        .queryParam("limit", 1)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode root = objectMapper.readTree(responseBody);

        // opensearch returns: ["query", ["Title1"], [""], ["url1"]]
        JsonNode titles = root.get(1);
        if (titles == null || titles.isEmpty()) {
            return null;
        }

        return titles.get(0).asText();
    }

    private String fetchSummary(String pageTitle) throws Exception {
        // Replace spaces with underscores for the REST API path
        String slug = pageTitle.replace(' ', '_');

        String responseBody = webClient.get()
                .uri("/api/rest_v1/page/summary/{title}", slug)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode root = objectMapper.readTree(responseBody);
        String extract = root.path("extract").asText(null);

        if (extract != null && !extract.isBlank()) {
            log.info("Got Wikipedia description for '{}' ({} chars)", pageTitle, extract.length());
            return extract;
        }

        return null;
    }
}
