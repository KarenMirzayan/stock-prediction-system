package kz.kbtu.newsservice.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
public class ArticleScraperService {

    private static final int TIMEOUT_MS = 10000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

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
            "share this article",
            "watch now",
            "squawk box",
            "squawk on the street",
            "closing bell",
            "power lunch",
            "fast money",
            "mad money"
    );

    private static final Set<String> NOISE_PATTERNS = Set.of(
            "video",
            "watch now"
    );

    private static final Set<String> PAYWALL_PATTERNS = Set.of(
            "as a subscriber to the cnbc investing club",
            "no fiduciary obligation or duty exists",
            "subject to our terms and conditions and privacy policy",
            "subscribe to unlock",
            "this content is for subscribers only",
            "you must be a subscriber",
            "sign up to read",
            "premium content"
    );

    public String scrapeArticle(String url) {
        try {
            log.info("Scraping article from: {}", url);

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();

            if (isPaywalled(doc)) {
                log.warn("Skipping paywalled article: {}", url);
                return null;
            }

            String content = extractArticleHtml(doc);

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

    private String extractArticleHtml(Document doc) {
        Element articleBody = findArticleBody(doc);

        if (articleBody == null) {
            articleBody = extractFallback(doc);
        }

        if (articleBody == null) {
            return null;
        }

        removeUnwantedElements(articleBody);

        // Extract only meaningful paragraph text instead of raw HTML
        StringBuilder sb = new StringBuilder();
        for (Element p : articleBody.select("p")) {
            String text = p.text().trim();
            if (text.isEmpty() || text.length() < 20) {
                continue;
            }
            if (shouldSkip(text) || isNoiseLine(text)) {
                continue;
            }
            sb.append(text).append("\n\n");
        }

        return sb.toString().trim();
    }

    private Element findArticleBody(Document doc) {
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

    private Element extractFallback(Document doc) {
        String[] fallbackSelectors = {"article", "main", "[role='main']", ".content"};

        for (String selector : fallbackSelectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty() && !elements.first().select("p").isEmpty()) {
                return elements.first();
            }
        }
        return null;
    }

    private void removeUnwantedElements(Element body) {
        body.select("script, style, nav, footer, header, aside, iframe, noscript, svg, button, form, input").remove();
        body.select("figure, figcaption, picture, video, audio, source").remove();
        body.select("[data-module='video'], [data-module='clip'], [class*='video'], [class*='Video']").remove();
        body.select("img").remove();

        for (Element el : body.select("*")) {
            String text = el.text().toLowerCase().trim();
            if (!text.isEmpty() && shouldSkip(text)) {
                el.remove();
            }
        }
    }

    private boolean isPaywalled(Document doc) {
        String text = doc.text().toLowerCase();
        for (String pattern : PAYWALL_PATTERNS) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNoiseLine(String text) {
        String lower = text.toLowerCase().trim();
        for (String noise : NOISE_PATTERNS) {
            if (lower.equals(noise)) {
                return true;
            }
        }
        // Skip lines that look like video timestamps (e.g., "04:57" or "4:57")
        if (lower.matches("^\\d{1,2}:\\d{2}$")) {
            return true;
        }
        return false;
    }

    private boolean shouldSkip(String text) {
        String lowerText = text.toLowerCase();

        for (String pattern : SKIP_PATTERNS) {
            if (lowerText.contains(pattern)) {
                return true;
            }
        }

        if (lowerText.matches(".*@\\w+.*contributed.*") ||
                lowerText.matches(".*follow.*@.*") ||
                lowerText.matches(".*\\bvia\\b.*@.*")) {
            return true;
        }

        return false;
    }
}
