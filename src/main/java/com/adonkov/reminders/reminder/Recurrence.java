package com.adonkov.reminders.reminder;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public enum Recurrence {

    NONE {
        @Override
        public Instant nextAfter(Instant from) {
            return null;
        }
    },
    DAILY {
        @Override
        public Instant nextAfter(Instant from) {
            return from.plus(1, ChronoUnit.DAYS);
        }
    },
    WEEKLY {
        @Override
        public Instant nextAfter(Instant from) {
            return from.plus(7, ChronoUnit.DAYS);
        }
    },
    MONTHLY {
        @Override
        public Instant nextAfter(Instant from) {
            // months aren't a fixed number of days, so go through a calendar date.
            // this clamps 31 Jan -> 28/29 Feb rather than spilling into March.
            return from.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
        }
    };

    /**
     * The next occurrence after {@code from}, or {@code null} for a one-off reminder.
     */
    public abstract Instant nextAfter(Instant from);

    public boolean repeats() {
        return this != NONE;
    }
}
