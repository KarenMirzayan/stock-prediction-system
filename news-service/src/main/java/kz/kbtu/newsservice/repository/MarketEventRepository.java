package kz.kbtu.newsservice.repository;

import kz.kbtu.common.entity.MarketEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MarketEventRepository extends JpaRepository<MarketEvent, Long> {

    boolean existsByTitleIgnoreCaseAndEventDate(String title, LocalDate eventDate);

    List<MarketEvent> findByEventDateGreaterThanEqualOrderByEventDateAsc(LocalDate from);

    List<MarketEvent> findByEventDateBetweenOrderByEventDateAsc(LocalDate from, LocalDate to);

    List<MarketEvent> findByCompany_TickerIgnoreCase(String ticker);
}
