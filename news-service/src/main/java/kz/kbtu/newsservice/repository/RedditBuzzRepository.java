package kz.kbtu.newsservice.repository;

import kz.kbtu.common.entity.RedditBuzz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedditBuzzRepository extends JpaRepository<RedditBuzz, Long> {
}
