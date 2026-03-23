package kz.kbtu.webapi.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePredictionRequest {

    private String direction;

    private String timeHorizon;

    @Min(0)
    @Max(100)
    private Integer confidence;

    @Size(max = 2000)
    private String rationale;

    private List<String> evidence;
}
