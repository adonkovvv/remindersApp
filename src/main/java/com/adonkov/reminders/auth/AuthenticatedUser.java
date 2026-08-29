package com.adonkov.reminders.auth;

/**
 * Principal placed on the security context by {@link JwtAuthenticationFilter}.
 * Built straight from the token claims so authenticated requests don't need a user lookup.
 */
public record AuthenticatedUser(Long id, String email) {
}
