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
public class SimulationSubmitResultDto {
    private int similarityScore;                   // 0-100
    private String feedback;                       // 1-2 sentences
    private List<PredictionDetailDto> predictions; // all individual predictions for the article
}
