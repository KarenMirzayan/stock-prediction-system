package kz.kbtu.newsservice.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import kz.kbtu.common.dto.RssArticleDto;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Element;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RssFeedService {

    public List<RssArticleDto> fetchFeed(String feedUrl) {
        List<RssArticleDto> articles = new ArrayList<>();

        try {
            log.info("Fetching RSS feed from: {}", feedUrl);
            URL url = URI.create(feedUrl).toURL();
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(url));

            log.info("Feed Title: {}", feed.getTitle());
            log.info("Feed Description: {}", feed.getDescription());
            log.info("Total entries: {}", feed.getEntries().size());

            for (SyndEntry entry : feed.getEntries()) {
                RssArticleDto article = convertToDto(entry);
                articles.add(article);
            }

            log.info("Successfully parsed {} articles from RSS feed", articles.size());

        } catch (Exception e) {
            log.error("Failed to fetch RSS feed from: {}", feedUrl, e);
        }

        return articles;
    }

    private RssArticleDto convertToDto(SyndEntry entry) {
        RssArticleDto dto = new RssArticleDto();

        dto.setTitle(entry.getTitle());
        dto.setUrl(entry.getLink());

        // Extract CNBC ID from <guid> or <metadata:id>
        String cnbcId = extractCnbcId(entry);
        dto.setExternalId(cnbcId);

        // Parse description - clean CDATA if present
        if (entry.getDescription() != null) {
            String description = entry.getDescription().getValue();
            // Clean up CDATA markers if present
            description = description.replaceAll("<!\\[CDATA\\[", "")
                    .replaceAll("\\]\\]>", "")
                    .trim();
            dto.setDescription(description);
        }

        // Parse published date
        if (entry.getPublishedDate() != null) {
            dto.setPublishedAt(entry.getPublishedDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime());
        }

        return dto;
    }

    /**
     * Extract CNBC article ID from the RSS entry.
     * Tries multiple sources:
     * 1. <guid> element (e.g., "108253940")
     * 2. <metadata:id> element
     * 3. Falls back to URI
     */
    private String extractCnbcId(SyndEntry entry) {
        // Try to get from guid first
        String guid = entry.getUri();
        if (guid != null && !guid.isEmpty()) {
            // CNBC guids are typically just numeric IDs
            // Clean up if it has "isPermaLink" artifacts
            guid = guid.trim();
            if (isNumericId(guid)) {
                return guid;
            }
        }

        // Try to extract from foreign markup (metadata:id)
        try {
            List<Element> foreignMarkup = entry.getForeignMarkup();
            for (Element element : foreignMarkup) {
                if ("id".equals(element.getName()) &&
                        element.getNamespace().getPrefix().equals("metadata")) {
                    return element.getText().trim();
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract metadata:id from entry", e);
        }

        // Fallback: try to extract ID from URL
        String link = entry.getLink();
        if (link != null) {
            // CNBC URLs often end with .html and have the article ID in the path
            // e.g., https://www.cnbc.com/2026/01/19/article-slug.html
            String[] parts = link.split("/");
            if (parts.length > 0) {
                String lastPart = parts[parts.length - 1];
                if (lastPart.endsWith(".html")) {
                    lastPart = lastPart.substring(0, lastPart.length() - 5);
                }
                // Use URL slug as fallback ID
                return "url-" + lastPart.hashCode();
            }
        }

        // Final fallback: use hashcode of link
        return "hash-" + (link != null ? link.hashCode() : entry.hashCode());
    }

    private boolean isNumericId(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}