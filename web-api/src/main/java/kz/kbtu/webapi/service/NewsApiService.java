package kz.kbtu.webapi.service;

import kz.kbtu.common.entity.*;
import kz.kbtu.webapi.dto.*;
import kz.kbtu.webapi.repository.ArticleRepository;
import kz.kbtu.webapi.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsApiService {

    private final ArticleRepository articleRepository;
    private final PredictionRepository predictionRepository;

    public NewsPageDto getLatestNews(int page, int size) {
        Page<Article> articles = articleRepository.findAnalyzedArticles(PageRequest.of(page, size));
        return toNewsPage(articles);
    }

    public NewsPageDto getNewsByCompany(String ticker, int page, int size) {
        Page<Article> articles = articleRepository.findByCompanyTicker(
                ticker.toUpperCase(), PageRequest.of(page, size));
        return toNewsPage(articles);
    }

    public NewsPageDto getNewsBySector(String sectorCode, int page, int size) {
        Page<Article> articles = articleRepository.findBySectorCode(
                sectorCode.toUpperCase(), PageRequest.of(page, size));
        return toNewsPage(articles);
    }

    public NewsPageDto getNewsBySentiment(String sentiment, int page, int size) {
        Article.Sentiment s = Article.Sentiment.valueOf(sentiment.toUpperCase());
        Page<Article> articles = articleRepository.findBySentiment(s, PageRequest.of(page, size));
        return toNewsPage(articles);
    }

    public Optional<NewsDetailDto> getNewsDetail(Long id) {
        return articleRepository.findByIdWithRelations(id)
                .map(this::toNewsDetail);
    }

    private NewsPageDto toNewsPage(Page<Article> page) {
        List<NewsItemDto> items = page.getContent().stream()
                .map(this::toNewsItem)
                .toList();

        return NewsPageDto.builder()
                .content(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    private NewsItemDto toNewsItem(Article article) {
        List<Prediction> predictions = predictionRepository.findByArticleId(article.getId());

        return NewsItemDto.builder()
                .id(article.getId())
                .headline(article.getTitle())
                .publishedAt(formatRelativeTime(article.getPublishedAt()))
                .publishedAtExact(formatAbsoluteTime(article.getPublishedAt()))
                .companies(mapCompanyPredictions(predictions))
                .tags(buildTags(article))
                .summary(article.getSummary())
                .sentiment(mapSentiment(article.getSentiment()))
                .sentimentScore(calculateSentimentScore(article.getSentiment(), predictions))
                .build();
    }

    private NewsDetailDto toNewsDetail(Article article) {
        List<Prediction> predictions = predictionRepository.findByArticleId(article.getId());

        return NewsDetailDto.builder()
                .id(article.getId())
                .headline(article.getTitle())
                .publishedAt(formatRelativeTime(article.getPublishedAt()))
                .publishedAtExact(formatAbsoluteTime(article.getPublishedAt()))
                .companies(mapCompanyPredictions(predictions))
                .tags(buildTags(article))
                .summary(article.getSummary())
                .sentiment(mapSentiment(article.getSentiment()))
                .sentimentScore(calculateSentimentScore(article.getSentiment(), predictions))
                .fullText(article.getContent())
                .analyticalExplanation(buildAnalyticalExplanation(predictions))
                .predictions(predictions.stream().map(this::toPredictionDetail).toList())
                .build();
    }

    private List<CompanyPredictionDto> mapCompanyPredictions(List<Prediction> predictions) {
        Map<String, CompanyPredictionDto> badgeMap = new LinkedHashMap<>();

        for (Prediction p : predictions) {
            String direction = mapDirection(p.getDirection());

            switch (p.getScope()) {
                case COMPANY -> {
                    if (p.getCompany() != null) {
                        String ticker = p.getCompany().getTicker();
                        badgeMap.putIfAbsent(ticker, CompanyPredictionDto.builder()
                                .ticker(ticker).direction(direction).build());
                    }
                }
                case MULTI_TICKER -> {
                    for (Company c : p.getCompanies()) {
                        badgeMap.putIfAbsent(c.getTicker(), CompanyPredictionDto.builder()
                                .ticker(c.getTicker()).direction(direction).build());
                    }
                }
                case SECTOR -> {
                    for (EconomySector sector : p.getSectors()) {
                        String label = buildSectorLabel(sector.getCode(), p.getCountries());
                        badgeMap.putIfAbsent(label, CompanyPredictionDto.builder()
                                .ticker(label).direction(direction).build());
                    }
                }
                case COUNTRY -> {
                    for (Country country : p.getCountries()) {
                        String label = buildCountryLabel(country.getCode(), p.getSectors());
                        badgeMap.putIfAbsent(label, CompanyPredictionDto.builder()
                                .ticker(label).direction(direction).build());
                    }
                }
            }
        }

        return new ArrayList<>(badgeMap.values());
    }

    private String buildSectorLabel(String sectorCode, Set<Country> countries) {
        if (countries == null || countries.isEmpty()) {
            return sectorCode;
        }
        return sectorCode + ":" + countries.stream()
                .map(Country::getCode)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private String buildCountryLabel(String countryCode, Set<EconomySector> sectors) {
        if (sectors == null || sectors.isEmpty()) {
            return countryCode;
        }
        return sectors.stream()
                .map(EconomySector::getCode)
                .sorted()
                .collect(Collectors.joining(",")) + ":" + countryCode;
    }

    private List<String> buildTags(Article article) {
        List<String> tags = new ArrayList<>();
        article.getMentionedSectors().stream()
                .map(EconomySector::getName)
                .forEach(tags::add);
        article.getMentionedCountries().stream()
                .map(Country::getName)
                .forEach(tags::add);
        return tags;
    }

    private String mapSentiment(Article.Sentiment sentiment) {
        if (sentiment == null) return "neutral";
        return switch (sentiment) {
            case POSITIVE -> "positive";
            case NEGATIVE -> "negative";
            case MIXED -> "neutral";
            case NEUTRAL -> "neutral";
        };
    }

    private int calculateSentimentScore(Article.Sentiment sentiment, List<Prediction> predictions) {
        if (predictions.isEmpty()) {
            return switch (sentiment != null ? sentiment : Article.Sentiment.NEUTRAL) {
                case POSITIVE -> 30;
                case NEGATIVE -> -30;
                case MIXED -> 5;
                case NEUTRAL -> 0;
            };
        }

        double avgConfidence = predictions.stream()
                .mapToInt(p -> p.getConfidence() != null ? p.getConfidence() : 50)
                .average()
                .orElse(50);

        long bullish = predictions.stream().filter(p -> p.getDirection() == Prediction.Direction.BULLISH).count();
        long bearish = predictions.stream().filter(p -> p.getDirection() == Prediction.Direction.BEARISH).count();
        long total = predictions.size();

        double directionFactor = (double) (bullish - bearish) / total;
        return (int) Math.round(directionFactor * avgConfidence);
    }

    private String mapDirection(Prediction.Direction direction) {
        return direction.name().toLowerCase();
    }

    private PredictionDetailDto toPredictionDetail(Prediction p) {
        List<String> targets = new ArrayList<>();
        if (p.getCompany() != null) {
            targets.add(p.getCompany().getTicker());
        }
        p.getCompanies().stream().map(Company::getTicker).forEach(targets::add);
        p.getSectors().stream().map(EconomySector::getCode).forEach(targets::add);
        p.getCountries().stream().map(Country::getCode).forEach(targets::add);

        return PredictionDetailDto.builder()
                .scope(p.getScope().name())
                .direction(p.getDirection().name())
                .timeHorizon(p.getTimeHorizon() != null ? p.getTimeHorizon().name() : null)
                .confidence(p.getConfidence() != null ? p.getConfidence() : 0)
                .rationale(p.getRationale())
                .targets(targets)
                .evidence(p.getEvidence())
                .build();
    }

    private String buildAnalyticalExplanation(List<Prediction> predictions) {
        if (predictions.isEmpty()) return "No analytical predictions available for this article.";

        return predictions.stream()
                .filter(p -> p.getRationale() != null && !p.getRationale().isEmpty())
                .map(p -> p.getRationale())
                .collect(Collectors.joining(" "));
    }

    private String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long hours = duration.toHours();
        if (hours < 1) return duration.toMinutes() + " minutes ago";
        if (hours < 24) return hours + " hours ago";
        long days = duration.toDays();
        if (days < 7) return days + " days ago";
        return dateTime.toLocalDate().toString();
    }

    private String formatAbsoluteTime(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";
        return dateTime.getMonth().name().charAt(0)
                + dateTime.getMonth().name().substring(1).toLowerCase()
                + " " + dateTime.getDayOfMonth()
                + ", " + dateTime.getYear()
                + " at " + String.format("%d:%02d %s",
                dateTime.getHour() == 0 ? 12 : (dateTime.getHour() > 12 ? dateTime.getHour() - 12 : dateTime.getHour()),
                dateTime.getMinute(),
                dateTime.getHour() < 12 ? "AM" : "PM");
    }
}
