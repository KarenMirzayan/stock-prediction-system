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
public class PredictionDetailDto {
    private String scope;
    private String direction;
    private String timeHorizon;
    private int confidence;
    private String rationale;
    private List<String> targets;
    private List<String> evidence;
}
