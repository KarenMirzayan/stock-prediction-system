// src/main/java/kz/kbtu/newsservice/service/ArticleScraperService.java
package kz.kbtu.newsservice.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class ArticleScraperService {

    private static final int TIMEOUT_MS = 10000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // Patterns to identify non-content text
    private static final Set<String> SKIP_PATTERNS = Set.of(
            "subscribe here",
            "sign up for",
            "click here",
            "read more",
            "related:",
            "see also:",
            "advertisement",
            "sponsored content",
            "follow us on",
            "share this article"
    );

    public String scrapeArticle(String url) {
        try {
            log.info("Scraping article from: {}", url);

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();

            String content = extractCnbcArticle(doc);

            if (content != null && !content.isEmpty()) {
                log.info("Successfully scraped article ({} characters)", content.length());
                return content;
            } else {
                log.warn("No content found for: {}", url);
                return null;
            }

        } catch (Exception e) {
            log.error("Failed to scrape article: {}", url, e);
            return null;
        }
    }

    private String extractCnbcArticle(Document doc) {
        StringBuilder content = new StringBuilder();
        Set<String> seenText = new HashSet<>(); // Track duplicates

        // Extract title
        String title = extractTitle(doc);
        if (title != null) {
            content.append("TITLE: ").append(title).append("\n\n");
        }

        // Extract subtitle/deck if present
        String subtitle = extractSubtitle(doc);
        if (subtitle != null && !subtitle.equals(title)) {
            content.append(subtitle).append("\n\n");
        }

        // Find the main article body container
        Element articleBody = findArticleBody(doc);

        if (articleBody != null) {
            // Process all content elements in order (p, li, h2, h3, blockquote)
            Elements contentElements = articleBody.select("p, li, h2, h3, h4, blockquote");

            for (Element element : contentElements) {
                String text = element.text().trim();

                // Skip if empty, duplicate, or matches skip patterns
                if (text.isEmpty() || seenText.contains(text) || shouldSkip(text)) {
                    continue;
                }

                // Skip very short text that's likely navigation/UI elements
                // But keep list items that are meaningful even if short
                if (text.length() < 20 && !element.tagName().equals("li")) {
                    continue;
                }

                // Format based on element type
                String formattedText = formatElement(element, text);
                if (formattedText != null) {
                    content.append(formattedText);
                    seenText.add(text);
                }
            }
        } else {
            // Fallback: try generic extraction
            content.append(extractFallback(doc, seenText));
        }

        return content.toString().trim();
    }

    private String extractTitle(Document doc) {
        // Try CNBC-specific selectors first
        String[] titleSelectors = {
                "h1.ArticleHeader-headline",
                "h1[data-testid='headline']",
                "h1.headline",
                "article h1",
                "h1"
        };

        for (String selector : titleSelectors) {
            Elements titles = doc.select(selector);
            if (!titles.isEmpty()) {
                String title = titles.first().text().trim();
                if (!title.isEmpty()) {
                    return title;
                }
            }
        }
        return null;
    }

    private String extractSubtitle(Document doc) {
        String[] subtitleSelectors = {
                ".ArticleHeader-headerContentContainer .deck",
                "[data-testid='deck']",
                ".article-deck",
                ".subtitle"
        };

        for (String selector : subtitleSelectors) {
            Elements subtitles = doc.select(selector);
            if (!subtitles.isEmpty()) {
                String subtitle = subtitles.first().text().trim();
                if (!subtitle.isEmpty()) {
                    return subtitle;
                }
            }
        }
        return null;
    }

    private Element findArticleBody(Document doc) {
        // Try CNBC-specific selectors
        String[] bodySelectors = {
                "div.ArticleBody-articleBody",
                "[data-testid='article-body']",
                "div.article-body",
                "div.group[data-module='article-body']",
                "article .group",
                "article"
        };

        for (String selector : bodySelectors) {
            Elements bodies = doc.select(selector);
            if (!bodies.isEmpty()) {
                return bodies.first();
            }
        }
        return null;
    }

    private boolean shouldSkip(String text) {
        String lowerText = text.toLowerCase();

        // Check skip patterns
        for (String pattern : SKIP_PATTERNS) {
            if (lowerText.contains(pattern)) {
                return true;
            }
        }

        // Skip social media handles and promotional text
        if (lowerText.matches(".*@\\w+.*contributed.*") ||
                lowerText.matches(".*follow.*@.*") ||
                lowerText.matches(".*\\bvia\\b.*@.*")) {
            return true;
        }

        return false;
    }

    private String formatElement(Element element, String text) {
        String tag = element.tagName();

        switch (tag) {
            case "h2":
            case "h3":
            case "h4":
                // Format headers
                return "\n## " + text + "\n\n";

            case "li":
                // Format list items - check if part of numbered or bullet list
                Element parent = element.parent();
                if (parent != null && parent.tagName().equals("ol")) {
                    int index = element.elementSiblingIndex() + 1;
                    return index + ". " + text + "\n";
                } else {
                    return "• " + text + "\n";
                }

            case "blockquote":
                return "> " + text + "\n\n";

            case "p":
            default:
                // Regular paragraph - add proper spacing
                return text + "\n\n";
        }
    }

    private String extractFallback(Document doc, Set<String> seenText) {
        StringBuilder content = new StringBuilder();

        // Try to get any paragraph content
        Elements paragraphs = doc.select("article p, main p, .content p");

        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (text.length() >= 30 && !seenText.contains(text) && !shouldSkip(text)) {
                content.append(text).append("\n\n");
                seenText.add(text);
            }
        }

        return content.toString();
    }
}