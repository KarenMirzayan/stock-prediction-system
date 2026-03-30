package kz.kbtu.webapi.service;

import kz.kbtu.common.entity.Prediction;
import kz.kbtu.common.entity.RedditBuzz;
import kz.kbtu.webapi.dto.CompanySentimentDto;
import kz.kbtu.webapi.dto.RedditBuzzDto;
import kz.kbtu.webapi.repository.PredictionRepository;
import kz.kbtu.webapi.repository.RedditBuzzRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanySentimentService {

    private final PredictionRepository predictionRepository;
    private final RedditBuzzRepository redditBuzzRepository;

    @Transactional(readOnly = true)
    public CompanySentimentDto getCompanySentiment(Long companyId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Prediction> predictions = predictionRepository.findRecentByCompanyId(companyId, since);

        int bullishCount = 0;
        int bearishCount = 0;
        int neutralCount = 0;

        double weightedSum = 0;
        double weightTotal = 0;

        for (Prediction p : predictions) {
            switch (p.getDirection()) {
                case BULLISH -> bullishCount++;
                case BEARISH -> bearishCount++;
                default -> neutralCount++;
            }
            int confidence = p.getConfidence() != null ? p.getConfidence() : 50;
            double directionValue = switch (p.getDirection()) {
                case BULLISH -> 1.0;
                case BEARISH -> -1.0;
                default -> 0.0;
            };
            weightedSum += directionValue * confidence;
            weightTotal += confidence;
        }

        int total = predictions.size();
        int score = weightTotal > 0 ? (int) Math.round((weightedSum / weightTotal) * 100) : 0;
        score = Math.max(-100, Math.min(100, score));

        String state;
        if (score > 15) state = "bullish";
        else if (score < -15) state = "bearish";
        else state = "stagnation";

        String description;
        if (total == 0) {
            description = "No recent predictions available";
        } else {
            int bullishPct = (int) Math.round((double) bullishCount / total * 100);
            description = switch (state) {
                case "bullish" -> bullishPct + "% of recent analyses indicate growth trends";
                case "bearish" -> (100 - bullishPct) + "% of recent analyses signal caution";
                default -> "Analysis signals are mixed with no clear direction";
            };
        }

        // Reddit buzz
        RedditBuzzDto buzzDto = redditBuzzRepository.findByCompanyId(companyId)
                .map(this::toBuzzDto)
                .orElse(null);

        return new CompanySentimentDto(score, state, description,
                total, bullishCount, bearishCount, neutralCount, buzzDto);
    }

    private RedditBuzzDto toBuzzDto(RedditBuzz buzz) {
        int rankChange = buzz.getRank24hAgo() != null ? buzz.getRank24hAgo() - buzz.getRank() : 0;
        int mentionChange = buzz.getMentions24hAgo() != null ? buzz.getMentions() - buzz.getMentions24hAgo() : 0;
        return new RedditBuzzDto(
                buzz.getRank(),
                buzz.getMentions(),
                buzz.getUpvotes(),
                buzz.getRank24hAgo(),
                buzz.getMentions24hAgo(),
                rankChange,
                mentionChange
        );
    }
}
