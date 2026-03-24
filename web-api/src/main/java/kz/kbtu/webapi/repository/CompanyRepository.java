package kz.kbtu.webapi.repository;

import kz.kbtu.common.entity.Company;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    @EntityGraph(attributePaths = {"sector", "country"})
    List<Company> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"sector", "country"})
    Optional<Company> findByTickerIgnoreCase(String ticker);

    @Query("""
            SELECT s.name, s.code, SUM(c.marketCap),
                   SUM(c.marketCap * c.dailyChangePercent) / NULLIF(SUM(c.marketCap), 0)
            FROM Company c JOIN c.sector s JOIN c.country co
            WHERE UPPER(co.code) = UPPER(:countryCode)
              AND c.marketCap IS NOT NULL AND c.dailyChangePercent IS NOT NULL
            GROUP BY s.name, s.code ORDER BY SUM(c.marketCap) DESC
            """)
    List<Object[]> findSectorHeatmapByCountry(@Param("countryCode") String countryCode);
}
