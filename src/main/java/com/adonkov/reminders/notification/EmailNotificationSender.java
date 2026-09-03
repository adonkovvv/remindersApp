package com.adonkov.reminders.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.notifications.sender", havingValue = "email")
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailNotificationSender(JavaMailSender mailSender,
                                   @org.springframework.beans.factory.annotation.Value("${app.notifications.from:noreply@localhost}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(Notification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(notification.recipientEmail());
        message.setSubject("Reminder: " + notification.title());
        message.setText(notification.body() == null ? notification.title() : notification.body());

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new NotificationException("Could not email reminder " + notification.reminderId(), ex);
        }
    }

    @Override
    public String name() {
        return "email";
    }
}
