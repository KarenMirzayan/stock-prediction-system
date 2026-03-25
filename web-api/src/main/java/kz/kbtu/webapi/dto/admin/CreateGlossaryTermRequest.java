package kz.kbtu.webapi.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateGlossaryTermRequest {

    @NotBlank
    private String term;

    private String definition;

    @NotBlank
    private String category;
}
