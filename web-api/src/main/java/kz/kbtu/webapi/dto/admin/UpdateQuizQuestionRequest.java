package kz.kbtu.webapi.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateQuizQuestionRequest {

    @NotBlank
    private String question;

    @NotNull
    private List<String> options;

    @NotNull
    @Min(0)
    private Integer correctAnswer;

    private String explanation;
}
