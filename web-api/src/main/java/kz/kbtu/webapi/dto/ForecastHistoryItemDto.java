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
public class ForecastHistoryItemDto {
    private Long id;
    private Long articleId;
    private String date;
    private String headline;
    private String forecast;
    private String actualMovement;
    private Boolean accurate;
    private List<String> companies;
}
