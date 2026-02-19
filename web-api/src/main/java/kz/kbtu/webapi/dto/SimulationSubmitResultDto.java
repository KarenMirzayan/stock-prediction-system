package kz.kbtu.webapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationSubmitResultDto {
    private int similarityScore;  // 0-100
    private String feedback;      // 1-2 sentences
    private String ourPrediction; // AI's analytical explanation, revealed after submission
}
