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
            // Create output directory if it doesn't exist
            Path outputPath = Paths.get(OUTPUT_DIR);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
                log.info("Created output directory: {}", OUTPUT_DIR);
            }

            // Generate filename from title
            String sanitizedTitle = sanitizeFileName(title);
            String timestamp = LocalDateTime.now().format(FILE_NAME_FORMATTER);
            String fileName = String.format("%s_%s.txt", timestamp, sanitizedTitle);

            Path filePath = outputPath.resolve(fileName);

            // Build file content
            StringBuilder fileContent = new StringBuilder();
            fileContent.append("=" .repeat(80)).append("\n");
            fileContent.append("TITLE: ").append(title).append("\n");
            fileContent.append("URL: ").append(url).append("\n");
            fileContent.append("SCRAPED AT: ").append(LocalDateTime.now()).append("\n");
            fileContent.append("=".repeat(80)).append("\n\n");
            fileContent.append(content);

            // Write to file
            Files.writeString(filePath, fileContent.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            log.info("Article saved to: {}", filePath.toAbsolutePath());

        } catch (IOException e) {
            log.error("Failed to save article to file: {}", title, e);
        }
    }

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
            fileContent.append("Companies: ").append(analysis.getOrDefault("companies", List.of())).append("\n");
            fileContent.append("Countries: ").append(analysis.getOrDefault("countries", List.of())).append("\n");
            fileContent.append("Sectors: ").append(analysis.getOrDefault("sectors", List.of())).append("\n");
            fileContent.append("Sentiment: ").append(analysis.getOrDefault("sentiment", "N/A")).append("\n\n");

            List<Map<String, Object>> predictions = (List<Map<String, Object>>)
                    analysis.getOrDefault("predictions", List.of());

            if (!predictions.isEmpty()) {
                fileContent.append("Predictions:\n");
                for (Map<String, Object> pred : predictions) {
                    fileContent.append(String.format("  - %s: %s (Confidence: %d%%) - %s\n",
                            pred.get("ticker"),
                            pred.get("direction"),
                            pred.get("confidence"),
                            pred.get("reasoning")));
                }
                fileContent.append("\n");
            }

            fileContent.append("=".repeat(80)).append("\n\n");
            fileContent.append(content);

            Files.writeString(filePath, fileContent.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            log.info("Article with analysis saved to: {}", filePath.toAbsolutePath());

        } catch (IOException e) {
            log.error("Failed to save article with analysis: {}", title, e);
        }
    }

    private String sanitizeFileName(String title) {
        // Remove invalid characters and limit length
        String sanitized = title.replaceAll("[^a-zA-Z0-9\\s-]", "")
                .replaceAll("\\s+", "_")
                .toLowerCase();

        // Limit to 50 characters
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