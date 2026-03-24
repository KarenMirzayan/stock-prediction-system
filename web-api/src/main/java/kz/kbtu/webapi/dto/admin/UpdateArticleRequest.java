package kz.kbtu.webapi.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateArticleRequest {

    @Size(max = 2000)
    private String summary;

    private String content;
}
