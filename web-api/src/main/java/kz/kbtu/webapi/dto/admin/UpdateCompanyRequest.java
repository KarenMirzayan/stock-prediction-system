package kz.kbtu.webapi.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCompanyRequest {

    @Size(max = 2000)
    private String description;

    @Size(max = 500)
    private String logoUrl;
}
