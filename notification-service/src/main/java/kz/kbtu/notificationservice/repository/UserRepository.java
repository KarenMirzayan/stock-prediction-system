package kz.kbtu.notificationservice.repository;

import kz.kbtu.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
        SELECT DISTINCT u FROM User u
        JOIN FETCH u.subscribedCompanies c
        WHERE u.telegramChatId IS NOT NULL
          AND c.ticker IN :tickers
    """)
    List<User> findTelegramUsersSubscribedToTickers(Set<String> tickers);
}
