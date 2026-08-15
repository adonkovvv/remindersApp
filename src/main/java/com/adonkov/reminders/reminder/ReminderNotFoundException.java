package com.adonkov.reminders.reminder;

public class ReminderNotFoundException extends RuntimeException {

    public ReminderNotFoundException(Long id) {
        super("Reminder %d not found".formatted(id));
    }
}
