package kz.kbtu.webapi.repository;

import kz.kbtu.common.entity.RedditBuzz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RedditBuzzRepository extends JpaRepository<RedditBuzz, Long> {

    Optional<RedditBuzz> findByCompanyId(Long companyId);
}
