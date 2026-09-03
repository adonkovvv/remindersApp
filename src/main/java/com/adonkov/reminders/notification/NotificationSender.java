package com.adonkov.reminders.notification;

/**
 * Implementations must be thread safe -- several worker threads call
 * {@link #send} concurrently.
 */
public interface NotificationSender {

    void send(Notification notification) throws NotificationException;

    String name();
}
