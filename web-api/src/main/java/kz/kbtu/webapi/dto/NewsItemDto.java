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
public class NewsItemDto {
    private Long id;
    private String headline;
    private String publishedAt;
    private String publishedAtExact;
    private List<CompanyPredictionDto> companies;
    private List<String> tags;
    private String summary;
    private String sentiment;   // "positive", "negative", "neutral"
    private int sentimentScore; // -100 to +100
}
