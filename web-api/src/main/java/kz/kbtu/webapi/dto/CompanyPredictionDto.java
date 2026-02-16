package kz.kbtu.webapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyPredictionDto {
    private String ticker;
    private String direction; // "bullish", "bearish", "neutral", "mixed", "volatile"
}
