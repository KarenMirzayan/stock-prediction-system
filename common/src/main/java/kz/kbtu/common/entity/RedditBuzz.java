package kz.kbtu.common.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reddit_buzz", indexes = {
    @Index(name = "idx_reddit_buzz_ticker", columnList = "ticker", unique = true),
    @Index(name = "idx_reddit_buzz_company", columnList = "company_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedditBuzz extends BaseEntity {

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "reddit_rank", nullable = false)
    private Integer rank;

    @Column(nullable = false)
    private Integer mentions;

    @Column(nullable = false)
    private Integer upvotes;

    @Column(name = "rank_24h_ago")
    private Integer rank24hAgo;

    @Column(name = "mentions_24h_ago")
    private Integer mentions24hAgo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;
}
