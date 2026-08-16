package com.adonkov.reminders.reminder;

import java.time.Instant;

public final class ReminderDtos {

    private ReminderDtos() {
    }

    public record CreateRequest(String title, String description, Instant dueAt) {
    }

    public record UpdateRequest(String title, String description, Instant dueAt, Boolean completed) {
    }

    public record Response(
            Long id,
            String title,
            String description,
            Instant dueAt,
            boolean completed,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(Reminder r) {
            return new Response(
                    r.getId(),
                    r.getTitle(),
                    r.getDescription(),
                    r.getDueAt(),
                    r.isCompleted(),
                    r.getCreatedAt(),
                    r.getUpdatedAt()
            );
        }
    }
}
