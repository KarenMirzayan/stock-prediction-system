package kz.kbtu.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_username", columnList = "username", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    @Column(name = "telegram_username", length = 100)
    private String telegramUsername;

    @Column(name = "telegram_link_token", length = 100)
    private String telegramLinkToken;

    @Column(name = "telegram_link_token_expiry")
    private LocalDateTime telegramLinkTokenExpiry;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_subscriptions",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "company_id")
    )
    @Builder.Default
    private Set<Company> subscribedCompanies = new HashSet<>();
}
