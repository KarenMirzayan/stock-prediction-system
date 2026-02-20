package kz.kbtu.newsservice.service;

import kz.kbtu.common.dto.ArticleAnalysisDto;
import kz.kbtu.common.entity.*;
import kz.kbtu.newsservice.repository.*;
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

        // Process mentioned companies (LLM now returns company names, not tickers)
        Set<Company> mentionedCompanies = new HashSet<>();
        if (analysis.getCompanies() != null) {
            for (String companyName : analysis.getCompanies()) {
                Company company = companyService.getOrCreateCompany(companyName);
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
                findCountry(countryName).ifPresent(mentionedCountries::add);
            }
        }
        article.setMentionedCountries(mentionedCountries);

        // Process mentioned sectors
        Set<EconomySector> mentionedSectors = new HashSet<>();
        if (analysis.getSectors() != null) {
            for (String sectorCode : analysis.getSectors()) {
                findSector(sectorCode).ifPresent(mentionedSectors::add);
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

        // Log for debugging
        log.info("Processed analysis for article: {} with {} predictions",
                article.getTitle(), article.getPredictions().size());
        for (Prediction p : article.getPredictions()) {
            log.info("  Prediction: scope={}, direction={}, confidence={}",
                    p.getScope(), p.getDirection(), p.getConfidence());
        }

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
                    .evidence(dto.getEvidence() != null ? new ArrayList<>(dto.getEvidence()) : new ArrayList<>());

            // Handle based on scope
            switch (scope) {
                case COMPANY:
                    // Single company - get first target (now a company name, not ticker)
                    if (dto.getTargets() != null && !dto.getTargets().isEmpty()) {
                        String companyName = dto.getTargets().get(0);
                        Company company = companyService.getOrCreateCompany(companyName);
                        if (company == null) {
                            log.info("Skipping COMPANY prediction — '{}' could not be verified as public", companyName);
                            return null;
                        }
                        builder.company(company);
                    }
                    break;

                case MULTI_TICKER:
                    // Multiple companies (targets are now company names)
                    Set<Company> companies = new HashSet<>();
                    if (dto.getTargets() != null) {
                        for (String companyName : dto.getTargets()) {
                            Company company = companyService.getOrCreateCompany(companyName);
                            if (company != null) {
                                companies.add(company);
                            }
                        }
                    }
                    if (companies.isEmpty()) {
                        log.info("Skipping MULTI_TICKER prediction — none of the targets could be verified as public");
                        return null;
                    }
                    builder.companies(companies);
                    break;

                case SECTOR:
                    // Sector prediction - targets are sector codes
                    Set<EconomySector> sectors = new HashSet<>();
                    if (dto.getTargets() != null) {
                        for (String sectorCode : dto.getTargets()) {
                            findSector(sectorCode).ifPresent(sectors::add);
                        }
                    }
                    // Also check dto.sectors field
                    if (dto.getSectors() != null) {
                        for (String sectorCode : dto.getSectors()) {
                            findSector(sectorCode).ifPresent(sectors::add);
                        }
                    }
                    builder.sectors(sectors);

                    // Countries affected — fall back to article-level countries if LLM omitted them
                    Set<Country> sectorCountries = new HashSet<>();
                    if (dto.getCountries() != null) {
                        for (String countryName : dto.getCountries()) {
                            findCountry(countryName).ifPresent(sectorCountries::add);
                        }
                    }
                    if (sectorCountries.isEmpty()) {
                        sectorCountries.addAll(article.getMentionedCountries());
                    }
                    builder.countries(sectorCountries);
                    break;

                case COUNTRY:
                    // Country prediction - targets are country names
                    Set<Country> countries = new HashSet<>();
                    if (dto.getTargets() != null) {
                        for (String countryName : dto.getTargets()) {
                            findCountry(countryName).ifPresent(countries::add);
                        }
                    }
                    // Also check dto.countries field
                    if (dto.getCountries() != null) {
                        for (String countryName : dto.getCountries()) {
                            findCountry(countryName).ifPresent(countries::add);
                        }
                    }
                    builder.countries(countries);

                    // Optional: sectors affected
                    Set<EconomySector> countrySectors = new HashSet<>();
                    if (dto.getSectors() != null) {
                        for (String sectorCode : dto.getSectors()) {
                            findSector(sectorCode).ifPresent(countrySectors::add);
                        }
                    }
                    builder.sectors(countrySectors);
                    break;
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to create prediction from DTO: {}", dto, e);
            return null;
        }
    }

    private Optional<Country> findCountry(String nameOrCode) {
        if (nameOrCode == null || nameOrCode.isEmpty()) {
            return Optional.empty();
        }
        // Try by code first (e.g., "US", "CN")
        return countryRepository.findByCode(nameOrCode.toUpperCase())
                .or(() -> countryRepository.findByNameIgnoreCase(nameOrCode));
    }

    private Optional<EconomySector> findSector(String codeOrName) {
        if (codeOrName == null || codeOrName.isEmpty()) {
            return Optional.empty();
        }
        // Try by code first (e.g., "TECH", "ENERGY")
        return sectorRepository.findByCode(codeOrName.toUpperCase())
                .or(() -> sectorRepository.findByNameIgnoreCase(codeOrName));
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
            // Handle old values that might still come through
            if ("ASSET_CLASS".equalsIgnoreCase(scope) || "MACRO_THEME".equalsIgnoreCase(scope)) {
                return Prediction.PredictionScope.SECTOR; // Default to SECTOR
            }
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

    public Set<String> findExistingCnbcIds(Collection<String> cnbcIds) {
        return articleRepository.findExistingCnbcIds(cnbcIds);
    }
}