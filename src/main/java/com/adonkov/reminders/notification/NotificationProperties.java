package com.adonkov.reminders.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(
        boolean enabled,
        Duration scanInterval,
        int batchSize,
        int workers,
        int queueCapacity,
        int maxAttempts,
        Duration defaultSnooze,
        Duration claimTimeout
) {
    public NotificationProperties {
        if (workers < 1) {
            throw new IllegalArgumentException("app.notifications.workers must be at least 1");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("app.notifications.batch-size must be at least 1");
        }
    }
}
