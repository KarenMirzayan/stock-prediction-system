package kz.kbtu.webapi.repository;

import kz.kbtu.common.entity.GlossaryTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlossaryTermRepository extends JpaRepository<GlossaryTerm, Long> {

    List<GlossaryTerm> findAllByOrderByTermAsc();

    List<GlossaryTerm> findByTermContainingIgnoreCaseOrDefinitionContainingIgnoreCase(
            String term, String definition);
}
