package kz.kbtu.newsservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ForecastHistoryItemDto {
    private String id;
    private String date;
    private String headline;
    private String forecast; // "growth", "decline", "stagnation"
    private String actualMovement;
    private boolean accurate;
    private List<String> companies;
}
