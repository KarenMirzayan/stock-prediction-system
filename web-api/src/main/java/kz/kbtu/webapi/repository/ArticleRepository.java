package kz.kbtu.webapi.repository;

import kz.kbtu.common.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    @Query("SELECT a FROM Article a WHERE a.isAnalyzed = true ORDER BY a.publishedAt DESC")
    Page<Article> findAnalyzedArticles(Pageable pageable);

    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.predictions LEFT JOIN FETCH a.mentionedCompanies " +
           "LEFT JOIN FETCH a.mentionedCountries LEFT JOIN FETCH a.mentionedSectors " +
           "WHERE a.id = :id")
    Optional<Article> findByIdWithRelations(@Param("id") Long id);

    @Query("SELECT a FROM Article a JOIN a.mentionedCompanies c " +
           "WHERE c.ticker = :ticker AND a.isAnalyzed = true ORDER BY a.publishedAt DESC")
    Page<Article> findByCompanyTicker(@Param("ticker") String ticker, Pageable pageable);

    @Query("SELECT a FROM Article a JOIN a.mentionedSectors s " +
           "WHERE s.code = :sectorCode AND a.isAnalyzed = true ORDER BY a.publishedAt DESC")
    Page<Article> findBySectorCode(@Param("sectorCode") String sectorCode, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.isAnalyzed = true AND a.sentiment = :sentiment ORDER BY a.publishedAt DESC")
    Page<Article> findBySentiment(@Param("sentiment") Article.Sentiment sentiment, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.mentionedSectors " +
           "WHERE a.isAnalyzed = true AND a.content IS NOT NULL AND a.summary IS NOT NULL " +
           "ORDER BY a.publishedAt DESC")
    List<Article> findSimulationArticles();
}
