package kz.kbtu.newsservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.kbtu.common.dto.ArticleAnalysisDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
@Slf4j
public class OllamaAnalysisService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:qwen2.5:14b}")
    private String model;

    public OllamaAnalysisService() {
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

    private String buildAnalysisPrompt(String title, String content) {
        return String.format("""
    You are a market inference analyst.

    Your task is NOT to summarize journalism or restate observed stock price movements.
    Your task is to infer potential FUTURE market impact based on factual information in the article.

    You must return ONLY valid JSON. No explanations outside JSON.

    TITLE: %s
    CONTENT: %s

    CRITICAL INSTRUCTIONS — READ CAREFULLY:

    1. READ THE ENTIRE ARTICLE before producing output.
       - If the article is retrospective, descriptive, or reports outcomes without introducing new,
         actionable information, it is NON-PREDICTIVE.
       - In such cases, set "predictions" to an empty array [].
       - "No prediction" is a valid and correct outcome.

    2. DO NOT mechanically copy reported stock movements as predictions.
       - You MAY use reported movements as forward-looking signals if there is a plausible trader reaction or market momentum effect.
       - Always justify with reasoning: why would the market react next?

    3. CREATE PREDICTIONS ONLY when there is a clear forward-looking causal signal, such as:
       - Earnings surprises with guidance implications
       - Regulatory or legal actions affecting future cash flows
       - Geopolitical events impacting commodities, supply chains, or risk appetite
       - Structural industry shifts (AI adoption, capex cycles, demand inflection points)
    
    4. When generating predictions, consider the following forward-looking signals:
       - Positive Earnings Reports: If a company reports positive earnings, it can be a significant bullish signal for short-term stock performance.
       - Forward-Looking Guidance: Include any forward-looking statements or significant financial metrics that exceed expectations and could impact future performance.
       - Market Sentiment Indicators: Consider immediate market sentiment indicators, such as earnings reports, analyst reactions, or reported investor sentiment, to capture potential short-term impacts.

    5. DETERMINE THE CORRECT SCOPE for each prediction:
       - COMPANY → single firm-specific catalyst
       - MULTI_TICKER → several companies affected by the same cause
       - SECTOR → industry-wide impact
       - ASSET_CLASS → commodities, bonds, FX, crypto
       - MACRO_THEME → geopolitics, regulation, monetary policy

    6. DO NOT force company-level predictions.
       - If impact is sectoral or macro, use SECTOR, MULTI_TICKER, ASSET_CLASS, or MACRO_THEME.
       - If impact is unclear or mixed, either mark direction as NEUTRAL or omit prediction.

    7. TIME HORIZON IS REQUIRED for every prediction:
       - SHORT_TERM (days to weeks)
       - MID_TERM (weeks to months)
       - LONG_TERM (months to years)

    8. CONFIDENCE RULES:
       - Use confidence of 80%% or higher only for strong, direct, and well-supported causal links
       - 60%%-75%% for reasonable but not guaranteed outcomes
       - Below 60%% only if uncertainty is explicitly high
       - Avoid exaggerated certainty

    9. EVIDENCE REQUIREMENT:
       - Every prediction must cite concrete facts from the article that justify the inference.
       - No external knowledge or assumptions beyond the article.

    10. COMPANIES FIELD:
        - List stock tickers (e.g., AAPL, GOOGL, TSLA) for companies mentioned in the article
        - Use uppercase standard ticker symbols

    REQUIRED JSON OUTPUT (no other text):

    {
      "summary": "Brief factual summary (2–4 sentences, no opinion)",
      "companies": ["TICKER1", "TICKER2"],
      "countries": ["Country1", "Country2"],
      "sectors": ["Technology", "Financial Services"],
      "sentiment": "POSITIVE | NEGATIVE | NEUTRAL | MIXED",
      "predictions": [
        {
          "scope": "COMPANY | MULTI_TICKER | SECTOR | ASSET_CLASS | MACRO_THEME",
          "targets": ["TSLA", "NVDA"],
          "direction": "BULLISH | BEARISH | NEUTRAL | MIXED | VOLATILE",
          "timeHorizon": "SHORT_TERM | MID_TERM | LONG_TERM",
          "confidence": 70,
          "rationale": "Forward-looking causal explanation based strictly on article facts",
          "evidence": [
            "Specific factual trigger cited from the article"
          ]
        }
      ]
    }

    REMEMBER:
    - Fewer, higher-quality predictions are better than many weak ones.
    - If a trader would not act on this information, do not produce a prediction.
    - Companies should be stock ticker symbols, not full names.
    """, title, content);
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