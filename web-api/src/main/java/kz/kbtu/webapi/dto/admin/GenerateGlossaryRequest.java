package kz.kbtu.webapi.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GenerateGlossaryRequest {

    @NotEmpty
    private List<TermEntry> terms;

    @Data
    public static class TermEntry {
        @NotNull
        private String term;
        @NotNull
        private String category;
    }
}
