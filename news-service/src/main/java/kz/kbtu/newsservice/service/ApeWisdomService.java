package kz.kbtu.newsservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ApeWisdomService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ApeWisdomService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://apewisdom.io/api/v1.0")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public record BuzzItem(
            int rank,
            String ticker,
            String name,
            int mentions,
            int upvotes,
            int rank24hAgo,
            int mentions24hAgo
    ) {}

    public List<BuzzItem> fetchAllPages() {
        List<BuzzItem> allItems = new ArrayList<>();
        int totalPages = 9;

        log.info("Starting ApeWisdom fetch ({} pages)...", totalPages);
        for (int page = 1; page <= totalPages; page++) {
            try {
                List<BuzzItem> pageItems = fetchPage(page);
                allItems.addAll(pageItems);
                log.info("  Page {}/{}: {} tickers (running total: {})", page, totalPages, pageItems.size(), allItems.size());

                if (pageItems.isEmpty()) {
                    log.info("  Page {} returned empty — stopping early", page);
                    break;
                }
                if (page < totalPages) {
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("  ApeWisdom fetch interrupted at page {}", page);
                break;
            } catch (Exception e) {
                log.warn("  Failed to fetch page {}/{}: {}", page, totalPages, e.getMessage());
            }
        }

        log.info("ApeWisdom fetch complete: {} total tickers across {} pages", allItems.size(), totalPages);
        return allItems;
    }

    private List<BuzzItem> fetchPage(int page) {
        String json = webClient.get()
                .uri("/filter/all-stocks/page/{page}", page)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (json == null) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) {
                return List.of();
            }

            List<BuzzItem> items = new ArrayList<>();
            for (JsonNode node : results) {
                items.add(new BuzzItem(
                        node.get("rank").asInt(),
                        node.get("ticker").asText(),
                        unescapeHtml(node.get("name").asText()),
                        node.get("mentions").asInt(),
                        node.get("upvotes").asInt(),
                        node.has("rank_24h_ago") ? node.get("rank_24h_ago").asInt() : 0,
                        node.has("mentions_24h_ago") ? node.get("mentions_24h_ago").asInt() : 0
                ));
            }
            return items;
        } catch (Exception e) {
            log.error("Failed to parse ApeWisdom response: {}", e.getMessage());
            return List.of();
        }
    }

    private String unescapeHtml(String text) {
        return text.replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&#39;", "'");
    }
}
