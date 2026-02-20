package kz.kbtu.webapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyPredictionDto {
    private String ticker;
    private String direction; // "bullish", "bearish", "neutral", "mixed", "volatile"
    private String rationale;
    private String timeHorizon; // e.g. "SHORT_TERM"
    private Integer confidence; // 0–100
    private List<String> evidence;
}
