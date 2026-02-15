package kz.kbtu.newsservice.service;

import kz.kbtu.common.entity.*;
import kz.kbtu.newsservice.dto.response.*;
import kz.kbtu.newsservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiDataService {

    private final ArticleRepository articleRepository;
    private final PredictionRepository predictionRepository;
    private final CompanyRepository companyRepository;
    private final EconomySectorRepository sectorRepository;

    public List<NewsItemDto> getArticles(int limit, String sentiment, String sector, String company) {
        List<Article> articles;

        if (sector != null && !sector.isEmpty()) {
            articles = articleRepository.findBySectorCode(sector);
        } else if (sentiment != null && !sentiment.isEmpty()) {
            Article.Sentiment sentimentEnum = parseFrontendSentiment(sentiment);
            if (sentimentEnum != null) {
                articles = articleRepository.findBySentiment(sentimentEnum);
            } else {
                articles = getAllAnalyzedArticles();
            }
        } else if (company != null && !company.isEmpty()) {
            articles = articleRepository.findByMentionedCompanyTicker(company);
        } else {
            articles = getAllAnalyzedArticles();
        }

        return articles.stream()
                .filter(Article::getIsAnalyzed)
                .sorted(Comparator.comparing(Article::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(this::toNewsItemDto)
                .collect(Collectors.toList());
    }

    public NewsDetailDto getArticleDetail(Long id) {
        Article article = articleRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new NoSuchElementException("Article not found: " + id));

        List<CompanyGrowthDto> companies = buildCompanyGrowthList(article);
        List<String> tags = buildTags(article);
        String analyticalExplanation = article.getPredictions().stream()
                .map(Prediction::getRationale)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));

        return NewsDetailDto.builder()
                .id(String.valueOf(article.getId()))
                .headline(article.getTitle())
                .publishedAt(formatPublishedDate(article.getPublishedAt()))
                .companies(companies)
                .tags(tags)
                .summary(article.getSummary() != null ? article.getSummary() : "")
                .sentiment(mapSentiment(article.getSentiment()))
                .sentimentScore(computeSentimentScore(article))
                .fullText(article.getContent() != null ? article.getContent() : "")
                .analyticalExplanation(analyticalExplanation)
                .build();
    }

    public List<HeatmapSectorDto> getHeatmapData() {
        List<EconomySector> sectors = sectorRepository.findAll();
        return sectors.stream()
                .map(this::toHeatmapSectorDto)
                .filter(dto -> !dto.getCompanies().isEmpty())
                .collect(Collectors.toList());
    }

    public List<SectorDataDto> getSectorsSummary() {
        List<EconomySector> sectors = sectorRepository.findAll();
        return sectors.stream()
                .map(this::toSectorDataDto)
                .collect(Collectors.toList());
    }

    public ForecastStatsDto getForecastStats() {
        List<Prediction> all = predictionRepository.findAll();
        int total = all.size();
        int growth = 0, decline = 0, stagnation = 0;

        for (Prediction p : all) {
            switch (mapDirection(p.getDirection())) {
                case "growth" -> growth++;
                case "decline" -> decline++;
                default -> stagnation++;
            }
        }

        return ForecastStatsDto.builder()
                .accuracy(0) // no actual verification system yet
                .totalForecasts(total)
                .growthForecasts(growth)
                .declineForecasts(decline)
                .stagnationForecasts(stagnation)
                .build();
    }

    public List<ForecastHistoryItemDto> getForecastHistory(int limit) {
        List<Prediction> predictions = predictionRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        return predictions.stream()
                .map(this::toForecastHistoryItemDto)
                .collect(Collectors.toList());
    }

    public List<String> getFilterCompanies() {
        return companyRepository.findAll().stream()
                .map(Company::getTicker)
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> getFilterSectors() {
        return sectorRepository.findAll().stream()
                .map(EconomySector::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    // --- Private helpers ---

    private List<Article> getAllAnalyzedArticles() {
        return articleRepository.findAll(Sort.by(Sort.Direction.DESC, "publishedAt"));
    }

    private NewsItemDto toNewsItemDto(Article article) {
        List<CompanyGrowthDto> companies = buildCompanyGrowthList(article);
        List<String> tags = buildTags(article);

        return NewsItemDto.builder()
                .id(String.valueOf(article.getId()))
                .headline(article.getTitle())
                .publishedAt(relativeTime(article.getPublishedAt()))
                .companies(companies)
                .tags(tags)
                .summary(article.getSummary() != null ? article.getSummary() : "")
                .sentiment(mapSentiment(article.getSentiment()))
                .sentimentScore(computeSentimentScore(article))
                .build();
    }

    private List<CompanyGrowthDto> buildCompanyGrowthList(Article article) {
        Map<String, List<Prediction>> predictionsByCompany = new HashMap<>();

        for (Prediction p : article.getPredictions()) {
            if (p.getCompany() != null) {
                predictionsByCompany
                        .computeIfAbsent(p.getCompany().getTicker(), k -> new ArrayList<>())
                        .add(p);
            }
            for (Company c : p.getCompanies()) {
                predictionsByCompany
                        .computeIfAbsent(c.getTicker(), k -> new ArrayList<>())
                        .add(p);
            }
        }

        return predictionsByCompany.entrySet().stream()
                .map(entry -> {
                    String ticker = entry.getKey();
                    List<Prediction> preds = entry.getValue();
                    Prediction first = preds.get(0);
                    double avgConfidence = preds.stream()
                            .mapToInt(p -> p.getConfidence() != null ? p.getConfidence() : 50)
                            .average().orElse(50);
                    String direction = mapDirection(first.getDirection());
                    double change = direction.equals("decline") ? -avgConfidence / 10.0 : avgConfidence / 10.0;
                    if (direction.equals("stagnation")) change = avgConfidence / 100.0;

                    return CompanyGrowthDto.builder()
                            .ticker(ticker)
                            .forecast(direction)
                            .change(Math.round(change * 10.0) / 10.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<String> buildTags(Article article) {
        List<String> tags = new ArrayList<>();
        for (EconomySector s : article.getMentionedSectors()) {
            tags.add(s.getName());
        }
        for (Country c : article.getMentionedCountries()) {
            tags.add(c.getName());
        }
        return tags;
    }

    private HeatmapSectorDto toHeatmapSectorDto(EconomySector sector) {
        List<Company> companies = companyRepository.findBySectorCode(sector.getCode());
        List<Article> sectorArticles = articleRepository.findBySectorCode(sector.getCode());
        int articleCount = sectorArticles.size();

        List<HeatmapCompanyDto> companyDtos = companies.stream()
                .map(c -> toHeatmapCompanyDto(c, sectorArticles))
                .collect(Collectors.toList());

        int avgSentiment = companyDtos.isEmpty() ? 50 :
                (int) companyDtos.stream().mapToInt(HeatmapCompanyDto::getSentiment).average().orElse(50);

        List<String> topics = sectorArticles.stream()
                .map(Article::getTitle)
                .limit(4)
                .map(title -> title.length() > 40 ? title.substring(0, 40) + "..." : title)
                .collect(Collectors.toList());

        return HeatmapSectorDto.builder()
                .id(sector.getCode().toLowerCase())
                .name(sector.getName())
                .sentiment(avgSentiment)
                .discussionVolume(articleCount)
                .companies(companyDtos)
                .topics(topics)
                .build();
    }

    private HeatmapCompanyDto toHeatmapCompanyDto(Company company, List<Article> sectorArticles) {
        // Compute average prediction confidence for this company across sector articles
        List<Prediction> companyPreds = sectorArticles.stream()
                .flatMap(a -> a.getPredictions().stream())
                .filter(p -> {
                    if (p.getCompany() != null && p.getCompany().getTicker().equals(company.getTicker())) return true;
                    return p.getCompanies().stream().anyMatch(c -> c.getTicker().equals(company.getTicker()));
                })
                .toList();

        int avgConfidence = companyPreds.isEmpty() ? 50 :
                (int) companyPreds.stream()
                        .mapToInt(p -> p.getConfidence() != null ? p.getConfidence() : 50)
                        .average().orElse(50);

        // Compute sentiment-weighted change
        double change = companyPreds.stream()
                .mapToDouble(p -> {
                    double conf = p.getConfidence() != null ? p.getConfidence() : 50;
                    return switch (p.getDirection()) {
                        case BULLISH -> conf / 10.0;
                        case BEARISH -> -conf / 10.0;
                        default -> conf / 100.0;
                    };
                })
                .average().orElse(0);

        return HeatmapCompanyDto.builder()
                .symbol(company.getTicker())
                .name(company.getName())
                .sentiment(avgConfidence)
                .change(Math.round(change * 10.0) / 10.0)
                .build();
    }

    private SectorDataDto toSectorDataDto(EconomySector sector) {
        List<Article> articles = articleRepository.findBySectorCode(sector.getCode());
        List<Prediction> predictions = articles.stream()
                .flatMap(a -> a.getPredictions().stream())
                .toList();

        int bullish = 0, bearish = 0;
        for (Prediction p : predictions) {
            if (p.getDirection() == Prediction.Direction.BULLISH) bullish++;
            else if (p.getDirection() == Prediction.Direction.BEARISH) bearish++;
        }

        String sentimentState;
        double change;
        if (bullish > bearish) {
            sentimentState = "bullish";
            change = predictions.isEmpty() ? 0 : predictions.stream()
                    .mapToInt(p -> p.getConfidence() != null ? p.getConfidence() : 50)
                    .average().orElse(0) / 10.0;
        } else if (bearish > bullish) {
            sentimentState = "bearish";
            change = predictions.isEmpty() ? 0 : -predictions.stream()
                    .mapToInt(p -> p.getConfidence() != null ? p.getConfidence() : 50)
                    .average().orElse(0) / 10.0;
        } else {
            sentimentState = "stagnation";
            change = 0;
        }

        return SectorDataDto.builder()
                .name(sector.getName())
                .marketCap(0) // no market cap data in our model
                .sentiment(sentimentState)
                .change(Math.round(change * 10.0) / 10.0)
                .build();
    }

    private ForecastHistoryItemDto toForecastHistoryItemDto(Prediction prediction) {
        String headline = prediction.getArticle() != null ? prediction.getArticle().getTitle() : "";
        List<String> companyTickers = new ArrayList<>();
        if (prediction.getCompany() != null) {
            companyTickers.add(prediction.getCompany().getTicker());
        }
        prediction.getCompanies().forEach(c -> companyTickers.add(c.getTicker()));

        return ForecastHistoryItemDto.builder()
                .id(String.valueOf(prediction.getId()))
                .date(formatShortDate(prediction.getCreatedAt()))
                .headline(headline)
                .forecast(mapDirection(prediction.getDirection()))
                .actualMovement("") // no actual movement tracking yet
                .accurate(false)     // no verification system yet
                .companies(companyTickers)
                .build();
    }

    private String mapDirection(Prediction.Direction direction) {
        if (direction == null) return "stagnation";
        return switch (direction) {
            case BULLISH -> "growth";
            case BEARISH -> "decline";
            case NEUTRAL, MIXED, VOLATILE -> "stagnation";
        };
    }

    private String mapSentiment(Article.Sentiment sentiment) {
        if (sentiment == null) return "neutral";
        return switch (sentiment) {
            case POSITIVE -> "positive";
            case NEGATIVE -> "negative";
            case MIXED, NEUTRAL -> "neutral";
        };
    }

    private Article.Sentiment parseFrontendSentiment(String sentiment) {
        return switch (sentiment.toLowerCase()) {
            case "positive" -> Article.Sentiment.POSITIVE;
            case "negative" -> Article.Sentiment.NEGATIVE;
            case "neutral" -> Article.Sentiment.NEUTRAL;
            default -> null;
        };
    }

    private int computeSentimentScore(Article article) {
        List<Prediction> predictions = article.getPredictions();
        if (predictions.isEmpty()) return 0;

        double avg = predictions.stream()
                .mapToDouble(p -> {
                    int conf = p.getConfidence() != null ? p.getConfidence() : 50;
                    return switch (p.getDirection()) {
                        case BULLISH -> conf;
                        case BEARISH -> -conf;
                        default -> 0;
                    };
                })
                .average().orElse(0);

        return (int) Math.round(avg);
    }

    private String relativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long minutes = duration.toMinutes();
        if (minutes < 60) return minutes + " minutes ago";
        long hours = duration.toHours();
        if (hours < 24) return hours + " hours ago";
        long days = duration.toDays();
        if (days < 30) return days + " days ago";
        return dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    private String formatPublishedDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a"));
    }

    private String formatShortDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }
}
