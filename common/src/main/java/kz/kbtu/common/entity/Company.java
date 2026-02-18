package kz.kbtu.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "companies", indexes = {
    @Index(name = "idx_company_ticker", columnList = "ticker", unique = true),
    @Index(name = "idx_company_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "ticker", nullable = false, length = 10, unique = true)
    private String ticker; // Stock ticker symbol (e.g., "AAPL", "GOOGL", "TSLA")

    @Column(name = "exchange", length = 20)
    private String exchange; // Exchange where the stock is listed (e.g., "NASDAQ", "NYSE")

    @Column(name = "description", length = 2000)
    private String description; // Company description from Wikipedia

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "market_cap")
    private Double marketCap; // In millions, from Finnhub

    @Column(name = "ipo_date", length = 10)
    private String ipoDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "company_sectors",
        joinColumns = @JoinColumn(name = "company_id"),
        inverseJoinColumns = @JoinColumn(name = "sector_id")
    )
    @Builder.Default
    private Set<EconomySector> sectors = new HashSet<>();

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Prediction> predictions = new HashSet<>();

    // Helper methods for managing sectors
    public void addSector(EconomySector sector) {
        this.sectors.add(sector);
        sector.getCompanies().add(this);
    }

    public void removeSector(EconomySector sector) {
        this.sectors.remove(sector);
        sector.getCompanies().remove(this);
    }
}