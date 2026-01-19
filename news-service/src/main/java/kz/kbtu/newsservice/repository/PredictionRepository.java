package kz.kbtu.newsservice.repository;

import kz.kbtu.common.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    
    List<Prediction> findByArticleId(Long articleId);
    
    List<Prediction> findByCompanyId(Long companyId);
    
    List<Prediction> findByCompanyTicker(String ticker);
    
    List<Prediction> findByDirection(Prediction.Direction direction);
    
    List<Prediction> findByScope(Prediction.PredictionScope scope);
    
    @Query("SELECT p FROM Prediction p WHERE p.confidence >= :minConfidence ORDER BY p.confidence DESC")
    List<Prediction> findHighConfidencePredictions(@Param("minConfidence") Integer minConfidence);
    
    @Query("SELECT p FROM Prediction p LEFT JOIN FETCH p.article WHERE p.company.ticker = :ticker ORDER BY p.createdAt DESC")
    List<Prediction> findByTickerWithArticle(@Param("ticker") String ticker);
    
    @Query("SELECT p FROM Prediction p WHERE p.scope = :scope AND p.direction = :direction")
    List<Prediction> findByScopeAndDirection(
        @Param("scope") Prediction.PredictionScope scope,
        @Param("direction") Prediction.Direction direction
    );
}