package com.adonkov.reminders.auth;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static AuthException emailTaken() {
        return new AuthException("That email is already registered", HttpStatus.CONFLICT);
    }

    public static AuthException badCredentials() {
        return new AuthException("Invalid email or password", HttpStatus.UNAUTHORIZED);
    }
}
