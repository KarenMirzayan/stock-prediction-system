package kz.kbtu.webapi.repository;

import kz.kbtu.common.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    @Query("SELECT DISTINCT p FROM Prediction p " +
           "LEFT JOIN FETCH p.company " +
           "LEFT JOIN FETCH p.companies " +
           "LEFT JOIN FETCH p.sectors " +
           "LEFT JOIN FETCH p.countries " +
           "WHERE p.article.id = :articleId")
    List<Prediction> findByArticleId(@Param("articleId") Long articleId);
}
