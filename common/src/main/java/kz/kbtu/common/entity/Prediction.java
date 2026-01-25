package kz.kbtu.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "predictions", indexes = {
        @Index(name = "idx_prediction_article", columnList = "article_id"),
        @Index(name = "idx_prediction_company", columnList = "company_id"),
        @Index(name = "idx_prediction_direction", columnList = "direction"),
        @Index(name = "idx_prediction_confidence", columnList = "confidence")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prediction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private PredictionScope scope;

    // For COMPANY scope - links to specific company
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    // For MULTI_TICKER scope - multiple companies
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "prediction_companies",
            joinColumns = @JoinColumn(name = "prediction_id"),
            inverseJoinColumns = @JoinColumn(name = "company_id")
    )
    @Builder.Default
    private Set<Company> companies = new HashSet<>();

    // For SECTOR scope - links to sector(s)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "prediction_sectors",
            joinColumns = @JoinColumn(name = "prediction_id"),
            inverseJoinColumns = @JoinColumn(name = "sector_id")
    )
    @Builder.Default
    private Set<EconomySector> sectors = new HashSet<>();

    // For COUNTRY scope - links to country/countries
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "prediction_countries",
            joinColumns = @JoinColumn(name = "prediction_id"),
            inverseJoinColumns = @JoinColumn(name = "country_id")
    )
    @Builder.Default
    private Set<Country> countries = new HashSet<>();

    // Prediction details
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private Direction direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_horizon", length = 20)
    private TimeHorizon timeHorizon;

    @Column(name = "confidence")
    private Integer confidence; // 0-100 percentage

    @Column(name = "rationale", length = 2000)
    private String rationale;

    @ElementCollection
    @CollectionTable(name = "prediction_evidence", joinColumns = @JoinColumn(name = "prediction_id"))
    @Column(name = "evidence", length = 1000)
    @Builder.Default
    private List<String> evidence = new ArrayList<>();

    // Enums
    public enum PredictionScope {
        COMPANY,        // Single company prediction
        MULTI_TICKER,   // Multiple companies affected by same cause
        SECTOR,         // Industry-wide impact (can include country)
        COUNTRY         // Country-level economic impact (e.g., trade war)
    }

    public enum Direction {
        BULLISH,
        BEARISH,
        NEUTRAL,
        MIXED,
        VOLATILE
    }

    public enum TimeHorizon {
        SHORT_TERM,  // Days to weeks
        MID_TERM,    // Weeks to months
        LONG_TERM    // Months to years
    }
}