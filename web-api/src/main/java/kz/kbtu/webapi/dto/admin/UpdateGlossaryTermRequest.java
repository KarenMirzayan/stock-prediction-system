package kz.kbtu.webapi.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateGlossaryTermRequest {

    @NotBlank
    private String definition;
}
