package kz.kbtu.webapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastStatsDto {
    private double accuracy;
    private int totalForecasts;
    private int growthForecasts;
    private int declineForecasts;
    private int stagnationForecasts;
    private Double growthAccuracy;
    private Double declineAccuracy;
    private Double stagnationAccuracy;
}
