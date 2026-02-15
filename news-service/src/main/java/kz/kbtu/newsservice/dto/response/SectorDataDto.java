package kz.kbtu.newsservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SectorDataDto {
    private String name;
    private double marketCap;
    private String sentiment; // "bullish", "bearish", "stagnation"
    private double change;
}
