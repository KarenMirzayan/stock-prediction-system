package kz.kbtu.newsservice.service;

import kz.kbtu.common.dto.ArticleAnalysisDto;
import kz.kbtu.common.entity.*;
import kz.kbtu.newsservice.repository.ArticleRepository;
import kz.kbtu.newsservice.repository.CountryRepository;
import kz.kbtu.newsservice.repository.EconomySectorRepository;
import kz.kbtu.newsservice.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final PredictionRepository predictionRepository;
    private final CompanyService companyService;
    private final CountryRepository countryRepository;
    private final EconomySectorRepository sectorRepository;

    @Transactional
    public Article createArticleFromRss(String cnbcId, String title, String url,
                                        String description, LocalDateTime publishedAt) {
        // Check if article already exists
        if (articleRepository.existsByCnbcId(cnbcId)) {
            log.info("Article {} already exists", cnbcId);
            return articleRepository.findByCnbcId(cnbcId).orElse(null);
        }

        Article article = Article.builder()
                .cnbcId(cnbcId)
                .title(title)
                .url(url)
                .description(description)
                .publishedAt(publishedAt)
                .isScraped(false)
                .isAnalyzed(false)
                .build();

        Article saved = articleRepository.save(article);
        log.info("Created article: {} (cnbcId: {})", title, cnbcId);
        
        return saved;
    }

    @Transactional
    public Article updateWithScrapedContent(Long articleId, String content) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found: " + articleId));

        article.setContent(content);
        article.setIsScraped(true);

        return articleRepository.save(article);
    }

    @Transactional
    public Article processAnalysis(Long articleId, ArticleAnalysisDto analysis, String modelName) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found: " + articleId));

        // Set basic analysis fields
        article.setSummary(analysis.getSummary());
        article.setSentiment(parseSentiment(analysis.getSentiment()));
        article.setAnalyzedAt(LocalDateTime.now());
        article.setAnalysisModel(modelName);
        article.setIsAnalyzed(true);

        // Process mentioned companies (get or create them)
        Set<Company> mentionedCompanies = new HashSet<>();
        if (analysis.getCompanies() != null) {
            for (String ticker : analysis.getCompanies()) {
                Company company = companyService.getOrCreateCompany(ticker);
                if (company != null) {
                    mentionedCompanies.add(company);
                }
            }
        }
        article.setMentionedCompanies(mentionedCompanies);

        // Process mentioned countries
        Set<Country> mentionedCountries = new HashSet<>();
        if (analysis.getCountries() != null) {
            for (String countryName : analysis.getCountries()) {
                countryRepository.findByNameIgnoreCase(countryName)
                        .or(() -> countryRepository.findByCode(countryName.toUpperCase()))
                        .ifPresent(mentionedCountries::add);
            }
        }
        article.setMentionedCountries(mentionedCountries);

        // Process mentioned sectors
        Set<EconomySector> mentionedSectors = new HashSet<>();
        if (analysis.getSectors() != null) {
            for (String sectorName : analysis.getSectors()) {
                sectorRepository.findByNameIgnoreCase(sectorName)
                        .or(() -> sectorRepository.findByCode(sectorName.toUpperCase()))
                        .ifPresent(mentionedSectors::add);
            }
        }
        article.setMentionedSectors(mentionedSectors);

        // Process predictions
        if (analysis.getPredictions() != null) {
            for (ArticleAnalysisDto.PredictionDto predDto : analysis.getPredictions()) {
                Prediction prediction = createPrediction(article, predDto);
                if (prediction != null) {
                    article.addPrediction(prediction);
                }
            }
        }

        Article saved = articleRepository.save(article);
        log.info("Processed analysis for article: {} with {} predictions", 
                article.getTitle(), article.getPredictions().size());

        return saved;
    }

    private Prediction createPrediction(Article article, ArticleAnalysisDto.PredictionDto dto) {
        try {
            Prediction.PredictionScope scope = parseScope(dto.getScope());
            Prediction.Direction direction = parseDirection(dto.getDirection());
            Prediction.TimeHorizon timeHorizon = parseTimeHorizon(dto.getTimeHorizon());

            Prediction.PredictionBuilder builder = Prediction.builder()
                    .article(article)
                    .scope(scope)
                    .direction(direction)
                    .timeHorizon(timeHorizon)
                    .confidence(dto.getConfidence())
                    .rationale(dto.getRationale())
                    .evidence(dto.getEvidence() != null ? new ArrayList<>(dto.getEvidence()) : new ArrayList<>())
                    .targets(dto.getTargets() != null ? new ArrayList<>(dto.getTargets()) : new ArrayList<>());

            // For COMPANY scope, link to the company entity
            if (scope == Prediction.PredictionScope.COMPANY && dto.getTargets() != null && !dto.getTargets().isEmpty()) {
                String ticker = dto.getTargets().get(0);
                Company company = companyService.getOrCreateCompany(ticker);
                if (company != null) {
                    builder.company(company);
                }
            }

            // For non-company scopes, set target identifier
            if (scope != Prediction.PredictionScope.COMPANY && dto.getTargets() != null && !dto.getTargets().isEmpty()) {
                builder.targetIdentifier(String.join(",", dto.getTargets()));
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to create prediction from DTO", e);
            return null;
        }
    }

    private Article.Sentiment parseSentiment(String sentiment) {
        if (sentiment == null) return Article.Sentiment.NEUTRAL;
        try {
            return Article.Sentiment.valueOf(sentiment.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Article.Sentiment.NEUTRAL;
        }
    }

    private Prediction.PredictionScope parseScope(String scope) {
        if (scope == null) return Prediction.PredictionScope.COMPANY;
        try {
            return Prediction.PredictionScope.valueOf(scope.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Prediction.PredictionScope.COMPANY;
        }
    }

    private Prediction.Direction parseDirection(String direction) {
        if (direction == null) return Prediction.Direction.NEUTRAL;
        try {
            return Prediction.Direction.valueOf(direction.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Prediction.Direction.NEUTRAL;
        }
    }

    private Prediction.TimeHorizon parseTimeHorizon(String timeHorizon) {
        if (timeHorizon == null) return Prediction.TimeHorizon.SHORT_TERM;
        try {
            return Prediction.TimeHorizon.valueOf(timeHorizon.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Prediction.TimeHorizon.SHORT_TERM;
        }
    }

    public Optional<Article> findByCnbcId(String cnbcId) {
        return articleRepository.findByCnbcId(cnbcId);
    }

    public Optional<Article> findByUrl(String url) {
        return articleRepository.findByUrl(url);
    }

    public boolean existsByCnbcId(String cnbcId) {
        return articleRepository.existsByCnbcId(cnbcId);
    }

    public List<Article> findUnanalyzedArticles() {
        return articleRepository.findByIsAnalyzedFalse();
    }

    public List<Article> findRecentArticles(int days) {
        return articleRepository.findRecentArticles(LocalDateTime.now().minusDays(days));
    }
}