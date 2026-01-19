package kz.kbtu.newsservice.repository;

import kz.kbtu.common.entity.EconomySector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EconomySectorRepository extends JpaRepository<EconomySector, Long> {
    
    Optional<EconomySector> findByCode(String code);
    
    Optional<EconomySector> findByNameIgnoreCase(String name);
    
    List<EconomySector> findByCodeIn(List<String> codes);
    
    boolean existsByCode(String code);
}