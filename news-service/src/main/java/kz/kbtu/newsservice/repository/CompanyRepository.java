package kz.kbtu.newsservice.repository;

import kz.kbtu.common.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    
    Optional<Company> findByTicker(String ticker);

    Optional<Company> findByNameIgnoreCase(String name);
    
    Optional<Company> findByTickerIgnoreCase(String ticker);
    
    List<Company> findByTickerIn(List<String> tickers);
    
    boolean existsByTicker(String ticker);
    
    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.sectors WHERE c.ticker = :ticker")
    Optional<Company> findByTickerWithSectors(@Param("ticker") String ticker);
    
    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.country WHERE c.ticker = :ticker")
    Optional<Company> findByTickerWithCountry(@Param("ticker") String ticker);
    
    @Query("SELECT c FROM Company c JOIN c.sectors s WHERE s.code = :sectorCode")
    List<Company> findBySectorCode(@Param("sectorCode") String sectorCode);
    
    @Query("SELECT c FROM Company c WHERE c.country.code = :countryCode")
    List<Company> findByCountryCode(@Param("countryCode") String countryCode);
}