package kz.kbtu.newsservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForecastStatsDto {
    private double accuracy;
    private int totalForecasts;
    private int growthForecasts;
    private int declineForecasts;
    private int stagnationForecasts;
}
