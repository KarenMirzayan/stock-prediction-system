package kz.kbtu.common.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "glossary_terms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlossaryTerm extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String term;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String definition;

    @Column(nullable = false, length = 100)
    private String category;
}
