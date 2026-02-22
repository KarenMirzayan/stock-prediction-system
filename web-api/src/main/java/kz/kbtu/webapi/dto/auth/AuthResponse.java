package kz.kbtu.webapi.dto.auth;

import kz.kbtu.webapi.dto.UserProfileDto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UserProfileDto user
) {
    public AuthResponse(String accessToken, String refreshToken, long expiresIn, UserProfileDto user) {
        this(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
