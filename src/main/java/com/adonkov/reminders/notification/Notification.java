package com.adonkov.reminders.notification;

import java.time.Instant;

/**
 * Everything a sender needs, captured while the claiming transaction is still open
 * so delivery can happen later on a worker thread without touching a JPA session.
 */
public record Notification(
        Long reminderId,
        Long userId,
        String recipientEmail,
        String title,
        String body,
        Instant dueAt
) {
}
