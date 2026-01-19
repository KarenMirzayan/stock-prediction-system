package kz.kbtu.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    // Target of prediction - can be company, sector, or broader scope
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private PredictionScope scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company; // For COMPANY scope predictions

    @Column(name = "target_identifier", length = 100)
    private String targetIdentifier; // For non-company targets (e.g., "SEMICONDUCTORS", "CRUDE_OIL")

    @ElementCollection
    @CollectionTable(name = "prediction_targets", joinColumns = @JoinColumn(name = "prediction_id"))
    @Column(name = "target")
    @Builder.Default
    private List<String> targets = new ArrayList<>(); // Multiple tickers for MULTI_TICKER scope

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
        MULTI_TICKER,   // Multiple companies affected
        SECTOR,         // Industry-wide impact
        ASSET_CLASS,    // Commodities, bonds, FX, crypto
        MACRO_THEME     // Geopolitics, regulation, monetary policy
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