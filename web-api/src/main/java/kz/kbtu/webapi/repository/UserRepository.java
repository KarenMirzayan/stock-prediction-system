package kz.kbtu.webapi.repository;

import kz.kbtu.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.subscribedCompanies c LEFT JOIN FETCH c.sectors LEFT JOIN FETCH c.country WHERE u.username = :username")
    Optional<User> findByUsernameWithSubscriptions(String username);

    Optional<User> findByTelegramLinkToken(String telegramLinkToken);
}
