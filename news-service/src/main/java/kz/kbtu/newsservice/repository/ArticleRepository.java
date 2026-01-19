package kz.kbtu.newsservice.repository;

import kz.kbtu.common.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    Optional<Article> findByCnbcId(String cnbcId);
    
    Optional<Article> findByUrl(String url);
    
    boolean existsByCnbcId(String cnbcId);
    
    boolean existsByUrl(String url);
    
    List<Article> findByIsAnalyzedFalse();
    
    List<Article> findByIsScrapedFalseAndIsAnalyzedFalse();
    
    @Query("SELECT a FROM Article a WHERE a.publishedAt >= :since ORDER BY a.publishedAt DESC")
    List<Article> findRecentArticles(@Param("since") LocalDateTime since);
    
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.predictions WHERE a.id = :id")
    Optional<Article> findByIdWithPredictions(@Param("id") Long id);
    
    @Query("SELECT a FROM Article a WHERE a.sentiment = :sentiment")
    List<Article> findBySentiment(@Param("sentiment") Article.Sentiment sentiment);
    
    @Query("SELECT a FROM Article a JOIN a.mentionedCompanies c WHERE c.ticker = :ticker ORDER BY a.publishedAt DESC")
    List<Article> findByMentionedCompanyTicker(@Param("ticker") String ticker);
}