package kz.kbtu.webapi.repository;

import kz.kbtu.common.entity.MarketEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MarketEventRepository extends JpaRepository<MarketEvent, Long> {

    @Query("SELECT e FROM MarketEvent e LEFT JOIN FETCH e.company " +
           "WHERE e.eventDate >= :from ORDER BY e.eventDate ASC, e.relevance ASC")
    List<MarketEvent> findUpcoming(@Param("from") LocalDate from);

    @Query("SELECT e FROM MarketEvent e LEFT JOIN FETCH e.company " +
           "WHERE e.eventDate BETWEEN :from AND :to ORDER BY e.eventDate ASC")
    List<MarketEvent> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
