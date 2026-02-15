package kz.kbtu.newsservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NewsDetailDto {
    private String id;
    private String headline;
    private String publishedAt;
    private List<CompanyGrowthDto> companies;
    private List<String> tags;
    private String summary;
    private String sentiment;
    private int sentimentScore;
    private String fullText;
    private String analyticalExplanation;
}
