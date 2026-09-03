package com.adonkov.reminders.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.notifications.sender", havingValue = "log", matchIfMissing = true)
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(Notification notification) {
        log.info("[{}] reminder {} for {} -- \"{}\" due {}",
                Thread.currentThread().getName(),
                notification.reminderId(),
                notification.recipientEmail(),
                notification.title(),
                notification.dueAt());
    }

    @Override
    public String name() {
        return "log";
    }
}
