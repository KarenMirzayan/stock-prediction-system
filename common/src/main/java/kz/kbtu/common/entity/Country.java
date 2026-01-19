package kz.kbtu.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "countries", indexes = {
    @Index(name = "idx_country_code", columnList = "code", unique = true),
    @Index(name = "idx_country_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, length = 3, unique = true)
    private String code; // ISO 3166-1 alpha-2 or alpha-3 code (e.g., "US", "USA")

    @Column(name = "region", length = 50)
    private String region; // e.g., "North America", "Europe", "Asia"

    @OneToMany(mappedBy = "country", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Company> companies = new HashSet<>();

    public Country(String name, String code) {
        this.name = name;
        this.code = code;
    }
}