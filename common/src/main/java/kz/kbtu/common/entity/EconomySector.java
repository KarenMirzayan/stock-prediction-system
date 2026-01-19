package kz.kbtu.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "economy_sectors", indexes = {
    @Index(name = "idx_sector_code", columnList = "code", unique = true),
    @Index(name = "idx_sector_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EconomySector extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name; // e.g., "Technology", "Healthcare", "Financial Services"

    @Column(name = "code", nullable = false, length = 20, unique = true)
    private String code; // e.g., "TECH", "HEALTH", "FINANCE"

    @Column(name = "description", length = 500)
    private String description;

    @ManyToMany(mappedBy = "sectors", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Company> companies = new HashSet<>();

    public EconomySector(String name, String code) {
        this.name = name;
        this.code = code;
    }
}