package kz.kbtu.webapi.service;

import kz.kbtu.common.entity.Company;
import kz.kbtu.common.entity.Prediction;
import kz.kbtu.common.entity.Prediction.Direction;
import kz.kbtu.webapi.dto.ForecastHistoryItemDto;
import kz.kbtu.webapi.dto.ForecastStatsDto;
import kz.kbtu.webapi.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ForecastAnalyticsService {

    private final PredictionRepository predictionRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    public ForecastStatsDto getStats() {
        List<Prediction> verified = predictionRepository.findVerifiedPredictions();

        long total = verified.size();
        if (total == 0) {
            return ForecastStatsDto.builder()
                    .accuracy(0)
                    .totalForecasts(0)
                    .build();
        }

        long accurateCount = verified.stream().filter(p -> Boolean.TRUE.equals(p.getAccurate())).count();

        long growth = verified.stream().filter(p -> p.getDirection() == Direction.BULLISH).count();
        long decline = verified.stream().filter(p -> p.getDirection() == Direction.BEARISH).count();
        long stagnation = verified.stream().filter(p -> p.getDirection() == Direction.NEUTRAL).count();

        return ForecastStatsDto.builder()
                .accuracy(round((double) accurateCount / total * 100))
                .totalForecasts((int) total)
                .growthForecasts((int) growth)
                .declineForecasts((int) decline)
                .stagnationForecasts((int) stagnation)
                .growthAccuracy(computeDirectionAccuracy(verified, Direction.BULLISH))
                .declineAccuracy(computeDirectionAccuracy(verified, Direction.BEARISH))
                .stagnationAccuracy(computeDirectionAccuracy(verified, Direction.NEUTRAL))
                .build();
    }

    public List<ForecastHistoryItemDto> getHistory() {
        return predictionRepository.findVerifiedPredictions().stream()
                .map(this::toHistoryItem)
                .toList();
    }

    private ForecastHistoryItemDto toHistoryItem(Prediction p) {
        String date = p.getArticle() != null && p.getArticle().getPublishedAt() != null
                ? p.getArticle().getPublishedAt().format(DATE_FMT)
                : p.getCreatedAt().format(DATE_FMT);

        String headline = p.getArticle() != null ? p.getArticle().getTitle() : "Unknown";

        List<String> companies;
        if (p.getScope() == Prediction.PredictionScope.COMPANY && p.getCompany() != null) {
            companies = List.of(p.getCompany().getTicker());
        } else if (p.getCompanies() != null && !p.getCompanies().isEmpty()) {
            companies = p.getCompanies().stream().map(Company::getTicker).toList();
        } else {
            companies = List.of();
        }

        return ForecastHistoryItemDto.builder()
                .id(p.getId())
                .date(date)
                .headline(headline)
                .forecast(mapDirection(p.getDirection()))
                .actualMovement(formatMovement(p.getActualMovementPct()))
                .accurate(p.getAccurate())
                .companies(companies)
                .build();
    }

    private String mapDirection(Direction direction) {
        return switch (direction) {
            case BULLISH -> "growth";
            case BEARISH -> "decline";
            case NEUTRAL -> "stagnation";
            default -> "stagnation";
        };
    }

    private String formatMovement(Double pct) {
        if (pct == null) return "N/A";
        String sign = pct >= 0 ? "+" : "";
        return sign + String.format("%.1f%%", pct);
    }

    private Double computeDirectionAccuracy(List<Prediction> verified, Direction direction) {
        List<Prediction> filtered = verified.stream()
                .filter(p -> p.getDirection() == direction)
                .toList();
        if (filtered.isEmpty()) return null;

        long accurate = filtered.stream().filter(p -> Boolean.TRUE.equals(p.getAccurate())).count();
        return round((double) accurate / filtered.size() * 100);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
