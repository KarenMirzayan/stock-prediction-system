package kz.kbtu.newsservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyGrowthDto {
    private String ticker;
    private String forecast; // "growth", "decline", "stagnation"
    private double change;
}
