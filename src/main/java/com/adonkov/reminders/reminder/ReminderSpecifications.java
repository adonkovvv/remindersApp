package com.adonkov.reminders.reminder;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

final class ReminderSpecifications {

    private ReminderSpecifications() {
    }

    static Specification<Reminder> ownedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    static Specification<Reminder> completedIs(Boolean completed) {
        if (completed == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("completed"), completed);
    }

    static Specification<Reminder> dueBefore(Instant instant) {
        if (instant == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThan(root.get("dueAt"), instant);
    }

    static Specification<Reminder> dueAfter(Instant instant) {
        if (instant == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThan(root.get("dueAt"), instant);
    }
}
