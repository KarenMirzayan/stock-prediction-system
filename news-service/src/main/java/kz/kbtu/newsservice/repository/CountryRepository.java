package kz.kbtu.newsservice.repository;

import kz.kbtu.common.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {
    
    Optional<Country> findByCode(String code);
    
    Optional<Country> findByNameIgnoreCase(String name);
    
    boolean existsByCode(String code);
}