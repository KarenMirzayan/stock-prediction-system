package kz.kbtu.webapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationScenarioDto {
    private Long id;
    private String title;
    private String date;         // "2024-03-20"
    private String period;       // "Q1 2024"
    private String newsHeadline;
    private String newsContent;
    private String context;      // AI-generated summary
    private String sector;
    private String difficulty;   // "Beginner" | "Intermediate" | "Advanced"
}
