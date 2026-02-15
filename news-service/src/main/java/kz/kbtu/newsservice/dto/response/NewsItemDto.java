package kz.kbtu.newsservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NewsItemDto {
    private String id;
    private String headline;
    private String publishedAt;
    private List<CompanyGrowthDto> companies;
    private List<String> tags;
    private String summary;
    private String sentiment; // "positive", "negative", "neutral"
    private int sentimentScore;
}
