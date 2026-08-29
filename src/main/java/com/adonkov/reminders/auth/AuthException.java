package com.adonkov.reminders.auth;

public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }

    public static AuthException emailTaken() {
        return new AuthException("That email is already registered");
    }

    public static AuthException badCredentials() {
        return new AuthException("Invalid email or password");
    }
}
