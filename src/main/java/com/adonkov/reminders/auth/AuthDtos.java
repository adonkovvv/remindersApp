package com.adonkov.reminders.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 100) String displayName
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record TokenResponse(String token, String tokenType, long expiresIn) {
        public static TokenResponse bearer(String token, long expiresIn) {
            return new TokenResponse(token, "Bearer", expiresIn);
        }
    }
}
