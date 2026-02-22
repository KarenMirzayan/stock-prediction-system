package kz.kbtu.webapi.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Size(min = 6, max = 100) String password,
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName
) {}
