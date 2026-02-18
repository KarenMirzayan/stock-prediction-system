package kz.kbtu.newsservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.kbtu.common.dto.ArticleAnalysisDto;
import kz.kbtu.common.entity.EconomySector;
import kz.kbtu.newsservice.repository.EconomySectorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
                .map(s -> s.getCode() + " (" + s.getName() + ")")
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
    - predictions.countries: Only for SECTOR scope if specific countries affected
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
                        "num_predict", 1500,
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