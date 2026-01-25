package kz.kbtu.newsservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FileStorageService {

    private static final String OUTPUT_DIR = "scraped-articles";
    private static final DateTimeFormatter FILE_NAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public void saveArticleToFile(String title, String url, String content) {
        try {
            Path outputPath = Paths.get(OUTPUT_DIR);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
                log.info("Created output directory: {}", OUTPUT_DIR);
            }

            String sanitizedTitle = sanitizeFileName(title);
            String timestamp = LocalDateTime.now().format(FILE_NAME_FORMATTER);
            String fileName = String.format("%s_%s.txt", timestamp, sanitizedTitle);

            Path filePath = outputPath.resolve(fileName);

            StringBuilder fileContent = new StringBuilder();
            fileContent.append("=".repeat(80)).append("\n");
            fileContent.append("TITLE: ").append(title).append("\n");
            fileContent.append("URL: ").append(url).append("\n");
            fileContent.append("SCRAPED AT: ").append(LocalDateTime.now()).append("\n");
            fileContent.append("=".repeat(80)).append("\n\n");
            fileContent.append(content);

            Files.writeString(filePath, fileContent.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            log.info("Article saved to: {}", filePath.toAbsolutePath());

        } catch (IOException e) {
            log.error("Failed to save article to file: {}", title, e);
        }
    }

    @SuppressWarnings("unchecked")
    public void saveArticleWithAnalysis(String title, String url, String content, Map<String, Object> analysis) {
        try {
            Path outputPath = Paths.get(OUTPUT_DIR);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            String sanitizedTitle = sanitizeFileName(title);
            String timestamp = LocalDateTime.now().format(FILE_NAME_FORMATTER);
            String fileName = String.format("%s_%s.txt", timestamp, sanitizedTitle);

            Path filePath = outputPath.resolve(fileName);

            StringBuilder fileContent = new StringBuilder();
            fileContent.append("=".repeat(80)).append("\n");
            fileContent.append("TITLE: ").append(title).append("\n");
            fileContent.append("URL: ").append(url).append("\n");
            fileContent.append("SCRAPED AT: ").append(LocalDateTime.now()).append("\n");
            fileContent.append("=".repeat(80)).append("\n\n");

            // Add LLM Analysis
            fileContent.append("=== LLM ANALYSIS ===\n\n");
            fileContent.append("Summary: ").append(analysis.getOrDefault("summary", "N/A")).append("\n\n");
            fileContent.append("Companies: ").append(formatList(analysis.get("companies"))).append("\n");
            fileContent.append("Countries: ").append(formatList(analysis.get("countries"))).append("\n");
            fileContent.append("Sectors: ").append(formatList(analysis.get("sectors"))).append("\n");
            fileContent.append("Sentiment: ").append(analysis.getOrDefault("sentiment", "N/A")).append("\n\n");

            List<Map<String, Object>> predictions = (List<Map<String, Object>>)
                    analysis.getOrDefault("predictions", List.of());

            if (!predictions.isEmpty()) {
                fileContent.append("=== PREDICTIONS ===\n\n");
                int predNum = 1;
                for (Map<String, Object> pred : predictions) {
                    fileContent.append(formatPrediction(pred, predNum++));
                }
            } else {
                fileContent.append("Predictions: None (article is non-predictive)\n\n");
            }

            fileContent.append("=".repeat(80)).append("\n\n");
            fileContent.append("=== FULL ARTICLE ===\n\n");
            fileContent.append(content);

            Files.writeString(filePath, fileContent.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            log.info("Article with analysis saved to: {}", filePath.toAbsolutePath());

        } catch (IOException e) {
            log.error("Failed to save article with analysis: {}", title, e);
        }
    }

    @SuppressWarnings("unchecked")
    private String formatPrediction(Map<String, Object> pred, int number) {
        StringBuilder sb = new StringBuilder();

        String scope = String.valueOf(pred.getOrDefault("scope", "UNKNOWN"));
        String direction = String.valueOf(pred.getOrDefault("direction", "NEUTRAL"));
        Object confidenceObj = pred.getOrDefault("confidence", 0);
        int confidence = confidenceObj instanceof Number ? ((Number) confidenceObj).intValue() : 0;
        String timeHorizon = String.valueOf(pred.getOrDefault("timeHorizon", "SHORT_TERM"));
        String rationale = String.valueOf(pred.getOrDefault("rationale", "N/A"));

        // Header
        sb.append(String.format("Prediction #%d [%s]\n", number, scope));
        sb.append("-".repeat(40)).append("\n");

        // Targets based on scope
        switch (scope.toUpperCase()) {
            case "COMPANY":
                sb.append("  Company: ").append(formatList(pred.get("targets"))).append("\n");
                break;
            case "MULTI_TICKER":
                sb.append("  Companies: ").append(formatList(pred.get("targets"))).append("\n");
                break;
            case "SECTOR":
                sb.append("  Sectors: ").append(formatList(pred.get("targets")));
                // Also check "sectors" field
                String additionalSectors = formatList(pred.get("sectors"));
                if (!additionalSectors.equals("[]") && !additionalSectors.isEmpty()) {
                    sb.append(", ").append(additionalSectors);
                }
                sb.append("\n");
                // Countries if present
                String sectorCountries = formatList(pred.get("countries"));
                if (!sectorCountries.equals("[]") && !sectorCountries.isEmpty()) {
                    sb.append("  Countries affected: ").append(sectorCountries).append("\n");
                }
                break;
            case "COUNTRY":
                sb.append("  Countries: ").append(formatList(pred.get("targets")));
                // Also check "countries" field
                String additionalCountries = formatList(pred.get("countries"));
                if (!additionalCountries.equals("[]") && !additionalCountries.isEmpty()) {
                    sb.append(", ").append(additionalCountries);
                }
                sb.append("\n");
                // Sectors if present
                String countrySectors = formatList(pred.get("sectors"));
                if (!countrySectors.equals("[]") && !countrySectors.isEmpty()) {
                    sb.append("  Sectors affected: ").append(countrySectors).append("\n");
                }
                break;
            default:
                sb.append("  Targets: ").append(formatList(pred.get("targets"))).append("\n");
        }

        // Direction and confidence
        sb.append(String.format("  Direction: %s\n", direction));
        sb.append(String.format("  Confidence: %d%%\n", confidence));
        sb.append(String.format("  Time Horizon: %s\n", timeHorizon));

        // Rationale
        sb.append(String.format("  Rationale: %s\n", rationale));

        // Evidence
        Object evidenceObj = pred.get("evidence");
        if (evidenceObj instanceof List && !((List<?>) evidenceObj).isEmpty()) {
            sb.append("  Evidence:\n");
            for (Object ev : (List<?>) evidenceObj) {
                sb.append(String.format("    • %s\n", ev));
            }
        }

        sb.append("\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String formatList(Object obj) {
        if (obj == null) {
            return "[]";
        }
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            if (list.isEmpty()) {
                return "[]";
            }
            return String.join(", ", (List<String>) list);
        }
        return String.valueOf(obj);
    }

    private String sanitizeFileName(String title) {
        String sanitized = title.replaceAll("[^a-zA-Z0-9\\s-]", "")
                .replaceAll("\\s+", "_")
                .toLowerCase();

        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }

        return sanitized;
    }

    public void saveAllArticlesSummary(int totalArticles, int successfulScrapes) {
        try {
            Path outputPath = Paths.get(OUTPUT_DIR);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            String timestamp = LocalDateTime.now().format(FILE_NAME_FORMATTER);
            String fileName = String.format("%s_SUMMARY.txt", timestamp);
            Path filePath = outputPath.resolve(fileName);

            StringBuilder summary = new StringBuilder();
            summary.append("RSS FEED SCRAPING SUMMARY\n");
            summary.append("=".repeat(80)).append("\n");
            summary.append("Timestamp: ").append(LocalDateTime.now()).append("\n");
            summary.append("Total articles in RSS feed: ").append(totalArticles).append("\n");
            summary.append("Successfully scraped: ").append(successfulScrapes).append("\n");
            summary.append("Failed scrapes: ").append(totalArticles - successfulScrapes).append("\n");
            summary.append("Success rate: ").append(
                    String.format("%.2f%%", (successfulScrapes * 100.0 / totalArticles))
            ).append("\n");

            Files.writeString(filePath, summary.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            log.info("Summary saved to: {}", filePath.toAbsolutePath());

        } catch (IOException e) {
            log.error("Failed to save summary", e);
        }
    }
}