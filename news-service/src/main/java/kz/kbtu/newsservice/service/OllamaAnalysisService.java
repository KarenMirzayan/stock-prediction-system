package kz.kbtu.newsservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.kbtu.common.dto.ArticleAnalysisDto;
import kz.kbtu.common.dto.MarketEventDto;
import kz.kbtu.common.entity.EconomySector;
import kz.kbtu.newsservice.repository.EconomySectorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OllamaAnalysisService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final EconomySectorRepository sectorRepository;

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:qwen2.5:14b}")
    private String model;

    public OllamaAnalysisService(EconomySectorRepository sectorRepository) {
        this.sectorRepository = sectorRepository;
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public ArticleAnalysisDto analyzeArticle(String title, String content) {
        log.info("Analyzing article with LLM: {}", title);

        String prompt = buildAnalysisPrompt(title, content);

        try {
            String response = generate(prompt);
            return parseAnalysisResponse(response);

        } catch (Exception e) {
            log.error("Failed to analyze article", e);
            return ArticleAnalysisDto.builder()
                    .summary("Analysis failed")
                    .sentiment("NEUTRAL")
                    .build();
        }
    }

    private String getAvailableSectorsForPrompt() {
        List<EconomySector> sectors = sectorRepository.findAll();

        return sectors.stream()
                .map(s -> s.getCode() + " (" + s.getName()
                        + (s.getDescription() != null ? ": " + s.getDescription() : "") + ")")
                .collect(Collectors.joining(", "));
    }

    private String buildAnalysisPrompt(String title, String content) {
        String availableSectors = getAvailableSectorsForPrompt();

        return String.format("""
    You are a market analyst. Analyze this article and produce predictions about future stock/market movements.

    TITLE: %s
    CONTENT: %s

    AVAILABLE SECTOR CODES: %s

    === OUTPUT RULES ===

    Return ONLY valid JSON. No text before or after.

    === SCOPE SELECTION LOGIC ===

    Ask yourself: "Who is DIRECTLY affected by this news?"

    1. Is a SPECIFIC COMPANY the main subject?
       → Use scope: "COMPANY", targets: ["Full Company Name"]
       → News about earnings, layoffs, products, lawsuits, management of ONE company = COMPANY scope

    2. Are MULTIPLE SPECIFIC COMPANIES directly named and affected?
       → Use scope: "MULTI_TICKER", targets: ["Company Name 1", "Company Name 2", ...]
   
    3. Is an ENTIRE INDUSTRY affected (not just one company)?
       → Use scope: "SECTOR", targets: ["SECTOR_CODE"]
       → Regulation affecting all companies in industry, industry-wide trends, commodity price changes
       → One company's problems do NOT make the whole sector bearish
   
    4. Is a COUNTRY'S ECONOMY affected (trade policy, sanctions, political instability)?
       → Use scope: "COUNTRY", targets: ["Country Name"]
       → Optionally include sectors: ["AFFECTED_SECTOR"] if specific industries impacted

    === CRITICAL LOGIC RULES ===

    RULE 1: Company news → Company prediction
    - If article is primarily about ONE company (layoffs, earnings, strategy change), predict for THAT COMPANY
    - Do NOT generalize to sector unless article explicitly discusses industry-wide impact
    
    RULE 2: Distinguish company vs sector impact
    - One company's internal changes (layoffs, restructuring, strategy shift) affect THAT COMPANY, not the entire sector
    - Industry-wide regulation, policy changes, or commodity price shifts affect the SECTOR
    - Ask: "Would competitors be affected the same way?" If no → COMPANY scope. If yes → SECTOR scope.
    
    RULE 3: Direction must match the target
    - If predicting for a company and news is bad for that company → that company BEARISH
    - If predicting for a sector, ask: "Is this bad for ALL companies in this sector?" If not, use COMPANY scope instead.
    
    RULE 4: Pivot/shift news requires nuance
    - When a company shifts strategy from one area to another, consider separate predictions for each impact
    - The abandoned area may face bearish pressure, the new focus area may see bullish sentiment
    
    RULE 5: No prediction is valid
    - If article is retrospective, opinion, or has no actionable forward signal → empty predictions array

    === CONFIDENCE GUIDELINES ===
    
    - 75-85%%: Direct, clear causal link between news and expected market movement
    - 60-74%%: Reasonable inference but outcome could go either way
    - Below 60%%: Speculative, use sparingly

    === JSON STRUCTURE ===

    {
      "summary": "2-4 sentence factual summary",
      "companies": ["Apple Inc", "Microsoft Corporation"],
      "countries": ["Country1"],
      "sectors": ["SECTOR_CODE1"],
      "sentiment": "POSITIVE | NEGATIVE | NEUTRAL | MIXED",
      "predictions": [
        {
          "scope": "COMPANY | MULTI_TICKER | SECTOR | COUNTRY",
          "targets": ["TARGET"],
          "countries": [],
          "sectors": [],
          "direction": "BULLISH | BEARISH | NEUTRAL | MIXED | VOLATILE",
          "timeHorizon": "SHORT_TERM | MID_TERM | LONG_TERM",
          "confidence": 70,
          "rationale": "Why this target will move in this direction",
          "evidence": ["Specific fact from article"]
        }
      ]
    }

    === FIELD RULES ===
    
    - companies: Full official company names mentioned in the article. Do NOT return stock tickers.
    - countries: Countries mentioned by name
    - sectors: Use CODES from the available sector list above, not full names
    - predictions.targets:
      - COMPANY scope → single full company name (e.g. "Apple Inc", NOT "AAPL")
      - MULTI_TICKER scope → multiple full company names
      - SECTOR scope → sector codes from available list
      - COUNTRY scope → country names
    - predictions.countries: REQUIRED for SECTOR scope. Always list the countries where this sector is affected. Never leave empty for SECTOR scope.
    - predictions.sectors: Only for COUNTRY scope if specific sectors affected
    """, title, content, availableSectors);
    }

    private String generate(String prompt) {
        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                        "temperature", 0.1,
                        "num_predict", 6000,
                        "top_p", 0.9
                )
        );

        Map<String, Object> response = webClient.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null && response.containsKey("response")) {
            return (String) response.get("response");
        }

        throw new RuntimeException("Invalid response from Ollama");
    }

    private ArticleAnalysisDto parseAnalysisResponse(String jsonResponse) {
        try {
            String cleaned = cleanJsonResponse(jsonResponse);
            log.debug("Cleaned JSON response: {}", cleaned);

            JsonNode root = objectMapper.readTree(cleaned);

            ArticleAnalysisDto.ArticleAnalysisDtoBuilder builder = ArticleAnalysisDto.builder()
                    .summary(root.path("summary").asText())
                    .sentiment(root.path("sentiment").asText("NEUTRAL"))
                    .companies(parseJsonArrayToList(root.path("companies")))
                    .countries(parseJsonArrayToList(root.path("countries")))
                    .sectors(parseJsonArrayToList(root.path("sectors")));

            // Parse predictions
            List<ArticleAnalysisDto.PredictionDto> predictions = new ArrayList<>();
            JsonNode predictionsNode = root.path("predictions");
            if (predictionsNode.isArray()) {
                for (JsonNode predNode : predictionsNode) {
                    ArticleAnalysisDto.PredictionDto pred = ArticleAnalysisDto.PredictionDto.builder()
                            .scope(predNode.path("scope").asText("COMPANY"))
                            .targets(parseJsonArrayToList(predNode.path("targets")))
                            .countries(parseJsonArrayToList(predNode.path("countries")))
                            .sectors(parseJsonArrayToList(predNode.path("sectors")))
                            .direction(predNode.path("direction").asText("NEUTRAL"))
                            .timeHorizon(predNode.path("timeHorizon").asText(
                                    predNode.path("time_horizon").asText("SHORT_TERM")))
                            .confidence(predNode.path("confidence").asInt(50))
                            .rationale(predNode.path("rationale").asText())
                            .evidence(parseJsonArrayToList(predNode.path("evidence")))
                            .build();
                    predictions.add(pred);
                }
            }
            builder.predictions(predictions);

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", jsonResponse, e);
            return ArticleAnalysisDto.builder()
                    .summary("Failed to parse response")
                    .sentiment("NEUTRAL")
                    .build();
        }
    }

    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private List<String> parseJsonArrayToList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> result.add(item.asText()));
        }
        return result;
    }

    /**
     * Extracts scheduled calendar events from a news article using a focused LLM prompt.
     * Returns an empty list if no qualifying events are found or parsing fails.
     *
     * @param title          article headline
     * @param content        full article text
     * @param companyTickers map of company name → ticker for companies already resolved in this article
     * @param articleDate    publication date of the article (for resolving relative dates)
     */
    public List<MarketEventDto> extractEvents(String title, String content,
                                              Map<String, String> companyTickers,
                                              LocalDate articleDate) {
        String prompt = buildEventExtractionPrompt(title, content, companyTickers, articleDate);
        try {
            String response = generate(prompt);
            return parseEventResponse(response);
        } catch (Exception e) {
            log.warn("Failed to extract events from article '{}': {}", title, e.getMessage());
            return List.of();
        }
    }

    private String buildEventExtractionPrompt(String title, String content,
                                               Map<String, String> companyTickers,
                                               LocalDate articleDate) {
        String today = LocalDate.now().toString();
        String articleDateStr = articleDate != null ? articleDate.toString() : today;

        StringBuilder tickerLines = new StringBuilder();
        if (companyTickers.isEmpty()) {
            tickerLines.append("(none identified)");
        } else {
            companyTickers.forEach((name, ticker) ->
                    tickerLines.append("  ").append(name).append(" → ").append(ticker).append("\n"));
        }

        return String.format("""
                You are extracting scheduled financial calendar events from a news article.

                TODAY: %s
                ARTICLE DATE: %s

                COMPANIES IN THIS ARTICLE (Name → Ticker):
                %s

                EXTRACT ONLY these event types when a specific future date is mentioned:
                - EARNINGS: Quarterly/annual earnings reports
                - DIVIDEND: Dividend payment or ex-dividend dates
                - CONFERENCE: Investor days, analyst days, shareholder meetings
                - ECONOMIC: Central bank meetings, major data releases (CPI, GDP, NFP, PMI, FOMC, etc.)

                RULES:
                - Only include events dated AFTER today (%s)
                - Resolve relative dates ("next Tuesday", "this Friday") using the ARTICLE DATE
                - Skip events with no clear specific date
                - relevance: HIGH = major market-moving, MEDIUM = sector-relevant, LOW = minor

                Respond ONLY with a JSON array. Return [] if nothing qualifies.

                [
                  {
                    "title": "Short descriptive name",
                    "date": "YYYY-MM-DD",
                    "time": "H:MM AM/PM ET or TBD",
                    "type": "EARNINGS | ECONOMIC | DIVIDEND | CONFERENCE",
                    "relevance": "HIGH | MEDIUM | LOW",
                    "companyTicker": "TICKER or null",
                    "sector": "Technology | Finance | Energy | Healthcare | Consumer | Industrial | etc"
                  }
                ]

                ARTICLE TITLE: %s
                ARTICLE: %s
                """,
                today, articleDateStr, tickerLines.toString(), today, title, content);
    }

    private List<MarketEventDto> parseEventResponse(String jsonResponse) {
        try {
            String cleaned = cleanJsonResponse(jsonResponse);
            JsonNode root = objectMapper.readTree(cleaned);

            List<MarketEventDto> events = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode node : root) {
                    String ticker = node.path("companyTicker").isNull() ? null
                            : node.path("companyTicker").asText(null);

                    MarketEventDto dto = MarketEventDto.builder()
                            .title(node.path("title").asText(null))
                            .date(node.path("date").asText(null))
                            .time(node.path("time").asText("TBD"))
                            .type(node.path("type").asText("ECONOMIC"))
                            .relevance(node.path("relevance").asText("MEDIUM"))
                            .companyTicker("null".equalsIgnoreCase(ticker) ? null : ticker)
                            .sector(node.path("sector").asText(null))
                            .build();

                    if (dto.getTitle() != null && dto.getDate() != null) {
                        events.add(dto);
                    }
                }
            }
            log.info("Extracted {} calendar events from article", events.size());
            return events;
        } catch (Exception e) {
            log.warn("Failed to parse event extraction response: {}", e.getMessage());
            return List.of();
        }
    }

    public boolean isAvailable() {
        try {
            webClient.get()
                    .uri("/")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getModelName() {
        return model;
    }
}