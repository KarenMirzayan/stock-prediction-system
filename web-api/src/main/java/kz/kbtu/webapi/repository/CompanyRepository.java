package kz.kbtu.webapi.repository;

import kz.kbtu.common.entity.Company;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    @EntityGraph(attributePaths = {"sectors", "country"})
    List<Company> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"sectors", "country"})
    Optional<Company> findByTickerIgnoreCase(String ticker);
}
