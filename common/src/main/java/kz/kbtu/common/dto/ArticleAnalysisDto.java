package kz.kbtu.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleAnalysisDto {
    private String summary;
    private String sentiment;

    @Builder.Default
    private List<String> companies = new ArrayList<>();

    @Builder.Default
    private List<String> countries = new ArrayList<>();
    
    @Builder.Default
    private List<String> sectors = new ArrayList<>();
    
    @Builder.Default
    private List<PredictionDto> predictions = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictionDto {
        private String scope;
        
        @Builder.Default
        private List<String> targets = new ArrayList<>();

        @Builder.Default
        private List<String> countries = new ArrayList<>();

        @Builder.Default
        private List<String> sectors = new ArrayList<>();

        private String direction;
        private String timeHorizon;
        private Integer confidence;
        private String rationale;

        @Builder.Default
        private List<String> evidence = new ArrayList<>();
    }
}