package kz.kbtu.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "articles", indexes = {
    @Index(name = "idx_article_cnbc_id", columnList = "cnbc_id", unique = true),
    @Index(name = "idx_article_url", columnList = "url"),
    @Index(name = "idx_article_published_at", columnList = "published_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article extends BaseEntity {

    // RSS metadata fields
    @Column(name = "cnbc_id", nullable = false, unique = true, length = 50)
    private String cnbcId; // From <guid> or <metadata:id>

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    @Column(name = "description", length = 2000)
    private String description; // From RSS <description>

    @Column(name = "published_at")
    private LocalDateTime publishedAt; // From RSS <pubDate>

    // Scraped content
    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // Full scraped article content

    // LLM analysis fields
    @Column(name = "summary", length = 2000)
    private String summary; // LLM-generated summary

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment", length = 20)
    private Sentiment sentiment;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(name = "analysis_model", length = 50)
    private String analysisModel; // e.g., "qwen2.5:14b"

    // Relationships
    @OneToMany(mappedBy = "article", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Prediction> predictions = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "article_companies",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "company_id")
    )
    @Builder.Default
    private Set<Company> mentionedCompanies = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "article_countries",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "country_id")
    )
    @Builder.Default
    private Set<Country> mentionedCountries = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "article_sectors",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "sector_id")
    )
    @Builder.Default
    private Set<EconomySector> mentionedSectors = new HashSet<>();

    // Processing status
    @Column(name = "is_scraped")
    @Builder.Default
    private Boolean isScraped = false;

    @Column(name = "is_analyzed")
    @Builder.Default
    private Boolean isAnalyzed = false;

    // Helper methods
    public void addPrediction(Prediction prediction) {
        predictions.add(prediction);
        prediction.setArticle(this);
    }

    public void removePrediction(Prediction prediction) {
        predictions.remove(prediction);
        prediction.setArticle(null);
    }

    public enum Sentiment {
        POSITIVE,
        NEGATIVE,
        NEUTRAL,
        MIXED
    }
}