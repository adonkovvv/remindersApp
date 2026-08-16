package com.adonkov.reminders.reminder;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ReminderDtos {

    private ReminderDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotNull @Future Instant dueAt
    ) {
    }

    public record UpdateRequest(
            @Size(min = 1, max = 200) String title,
            @Size(max = 2000) String description,
            Instant dueAt,
            Boolean completed
    ) {
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
