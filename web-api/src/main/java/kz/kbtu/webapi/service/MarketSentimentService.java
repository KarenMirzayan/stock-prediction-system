package kz.kbtu.webapi.service;

import kz.kbtu.common.entity.Prediction;
import kz.kbtu.webapi.dto.MarketSentimentDto;
import kz.kbtu.webapi.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketSentimentService {

    private final PredictionRepository predictionRepository;

    @Transactional(readOnly = true)
    public MarketSentimentDto getMarketSentiment() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Prediction> predictions = predictionRepository.findRecentCompanyPredictions(since);

        if (predictions.isEmpty()) {
            return new MarketSentimentDto(0, "stagnation",
                    "No recent predictions available", List.of(),
                    0, 0, 0, 0);
        }

        int bullishCount = 0;
        int bearishCount = 0;
        int neutralCount = 0;

        // Group predictions by sector
        Map<String, List<Prediction>> bySector = new LinkedHashMap<>();

        for (Prediction p : predictions) {
            switch (p.getDirection()) {
                case BULLISH -> bullishCount++;
                case BEARISH -> bearishCount++;
                default -> neutralCount++;
            }

            String sectorName = p.getCompany() != null && p.getCompany().getSector() != null
                    ? p.getCompany().getSector().getName()
                    : "Other";
            bySector.computeIfAbsent(sectorName, k -> new ArrayList<>()).add(p);
        }

        int total = predictions.size();

        // Compute overall score: -100 to +100
        // bullish = +1, bearish = -1, neutral/mixed/volatile = 0, weighted by confidence
        double weightedSum = 0;
        double weightTotal = 0;
        for (Prediction p : predictions) {
            int confidence = p.getConfidence() != null ? p.getConfidence() : 50;
            double directionValue = switch (p.getDirection()) {
                case BULLISH -> 1.0;
                case BEARISH -> -1.0;
                default -> 0.0;
            };
            weightedSum += directionValue * confidence;
            weightTotal += confidence;
        }
        int score = weightTotal > 0 ? (int) Math.round((weightedSum / weightTotal) * 100) : 0;
        score = Math.max(-100, Math.min(100, score));

        // Determine state
        String state;
        if (score > 15) state = "bullish";
        else if (score < -15) state = "bearish";
        else state = "stagnation";

        // Build description
        int bullishPct = (int) Math.round((double) bullishCount / total * 100);
        String description = switch (state) {
            case "bullish" -> bullishPct + "% of recent analyses indicate growth trends";
            case "bearish" -> (100 - bullishPct) + "% of recent analyses signal caution";
            default -> "Market signals are mixed with no clear direction";
        };

        // Build sector-based factors (top 4 sectors by prediction count)
        List<String> factors = bySector.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, List<Prediction>>, Integer>comparing(
                        e -> e.getValue().size()).reversed())
                .limit(4)
                .map(entry -> buildSectorFactor(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return new MarketSentimentDto(score, state, description, factors,
                total, bullishCount, bearishCount, neutralCount);
    }

    private String buildSectorFactor(String sectorName, List<Prediction> predictions) {
        long bullish = predictions.stream()
                .filter(p -> p.getDirection() == Prediction.Direction.BULLISH).count();
        long bearish = predictions.stream()
                .filter(p -> p.getDirection() == Prediction.Direction.BEARISH).count();
        int total = predictions.size();

        if (bullish > bearish && bullish > total / 2.0) {
            return sectorName + " showing bullish momentum (" + bullish + "/" + total + ")";
        } else if (bearish > bullish && bearish > total / 2.0) {
            return sectorName + " trending bearish (" + bearish + "/" + total + " decline)";
        } else {
            return sectorName + " mixed signals (" + bullish + " bullish, " + bearish + " bearish)";
        }
    }
}
