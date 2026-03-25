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
                    "options", Map.of("temperature", 0.0, "num_predict", 3000)
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

    /**
     * Generates a concise financial glossary definition for a given term.
     *
     * @return the definition text, or null if Ollama is unavailable
     */
    public String generateDefinition(String term, String category) {
        String prompt = String.format(
                "You are a financial education assistant. " +
                "Write a clear, concise definition (2-3 sentences) for the following financial/market term.\n\n" +
                "Term: %s\nCategory: %s\n\n" +
                "Respond with ONLY the definition text. No prefix, no quotes, no \"Definition:\" label.",
                term, category
        );

        try {
            Map<String, Object> request = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false,
                    "options", Map.of("temperature", 0.3, "num_predict", 300)
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/api/generate")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("response")) {
                String raw = (String) response.get("response");
                return raw
                        .replaceAll("(?s)<think>.*?</think>", "")
                        .replaceAll("(?s)<think>.*$", "")
                        .trim();
            }
        } catch (Exception e) {
            log.warn("Ollama unavailable for glossary generation: {}", e.getMessage());
        }
        return null;
    }

    private int parseScore(String raw) {
        // Strip complete <think>...</think> blocks, then any unclosed <think>... tail
        // (unclosed = truncated response that never emitted </think>)
        String cleaned = raw
                .replaceAll("(?s)<think>.*?</think>", "")
                .replaceAll("(?s)<think>.*$", "")
                .trim();

        // Take the last number ≤ 100 — models often say "score is X" at the end
        Matcher m = Pattern.compile("\\b([0-9]{1,3})\\b").matcher(cleaned);
        int score = -1;
        while (m.find()) {
            int candidate = Integer.parseInt(m.group(1));
            if (candidate <= 100) score = candidate;
        }
        if (score >= 0) return score;

        log.warn("Could not parse similarity score from Ollama response: {}", raw);
        return -1;
    }
}
