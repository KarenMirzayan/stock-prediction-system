package kz.kbtu.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "market_events", indexes = {
    @Index(name = "idx_market_event_date", columnList = "event_date"),
    @Index(name = "idx_market_event_company", columnList = "company_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketEvent extends BaseEntity {

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_time", length = 50)
    private String eventTime; // e.g. "4:30 PM ET" or "TBD"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Relevance relevance;

    @Column(length = 100)
    private String sector;

    // Null for macro/economic events (CPI, GDP, Fed meeting, etc.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    // Article that first mentioned this event
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_article_id")
    private Article sourceArticle;

    public enum EventType {
        EARNINGS,
        ECONOMIC,
        DIVIDEND,
        CONFERENCE
    }

    public enum Relevance {
        HIGH,
        MEDIUM,
        LOW
    }
}
