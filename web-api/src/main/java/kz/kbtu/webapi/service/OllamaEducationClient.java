package kz.kbtu.webapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class OllamaEducationClient {

    private final RestClient restClient;

    @Value("${ollama.model:qwen2.5:14b}")
    private String model;

    public OllamaEducationClient(@Value("${ollama.url:http://localhost:11434}") String ollamaUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(ollamaUrl)
                .build();
    }

    /**
     * Asks the LLM to rate the semantic similarity between two prediction texts.
     *
     * @return a score 0-100, or -1 if Ollama is unavailable (caller should fall back)
     */
    public int scoreSimilarity(String userPrediction, String ourPrediction) {
        String prompt = String.format(
                "You are evaluating how semantically similar two market predictions are.\n\n" +
                "PREDICTION A (user): %s\n\n" +
                "PREDICTION B (AI analysis): %s\n\n" +
                "Rate their semantic similarity from 0 to 100 where:\n" +
                "- 100 = identical meaning expressed in different words\n" +
                "- 70-99 = same direction and key drivers identified\n" +
                "- 40-69 = partially correct, some key factors matched\n" +
                "- 0-39 = different direction or misses the key drivers\n\n" +
                "Respond with ONLY a single integer number between 0 and 100. No explanation, no text.",
                userPrediction, ourPrediction
        );

        try {
            Map<String, Object> request = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false,
                    "options", Map.of("temperature", 0.0, "num_predict", 500)
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/api/generate")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("response")) {
                return parseScore((String) response.get("response"));
            }
        } catch (Exception e) {
            log.warn("Ollama unavailable for similarity scoring: {}", e.getMessage());
        }
        return -1;
    }

    private int parseScore(String raw) {
        // Strip <think>...</think> blocks produced by reasoning models (e.g. deepseek-r1)
        String cleaned = raw.replaceAll("(?s)<think>.*?</think>", "").trim();

        // Extract the first 1-3 digit number from the response
        Matcher m = Pattern.compile("\\b([0-9]{1,3})\\b").matcher(cleaned);
        if (m.find()) {
            int score = Integer.parseInt(m.group(1));
            return Math.min(100, Math.max(0, score));
        }
        log.warn("Could not parse similarity score from Ollama response: {}", raw);
        return -1;
    }
}
