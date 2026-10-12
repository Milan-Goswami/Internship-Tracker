package com.interntrack.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UserResponse user
) {
    public static AuthResponse of(String accessToken, Long expiresInSeconds, UserResponse user) {
        return new AuthResponse(accessToken, null, "Bearer", expiresInSeconds, user);
    }

    public static AuthResponse of(String accessToken, String refreshToken, Long expiresInSeconds, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}