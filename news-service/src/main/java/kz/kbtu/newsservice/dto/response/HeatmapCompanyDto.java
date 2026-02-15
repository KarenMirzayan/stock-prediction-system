package kz.kbtu.newsservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HeatmapCompanyDto {
    private String symbol;
    private String name;
    private int sentiment;
    private double change;
}
