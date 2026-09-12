package com.adonkov.reminders.notification;

import com.adonkov.reminders.AbstractPostgresTest;
import com.adonkov.reminders.reminder.Recurrence;
import com.adonkov.reminders.reminder.Reminder;
import com.adonkov.reminders.reminder.ReminderRepository;
import com.adonkov.reminders.user.User;
import com.adonkov.reminders.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.notifications.enabled=true",
        "app.notifications.initial-delay=1h",
        "app.notifications.batch-size=50"
})
class ReminderDispatcherIT extends AbstractPostgresTest {

    @Autowired
    private ReminderDispatcher dispatcher;

    @Autowired
    private ReminderRepository reminders;

    @Autowired
    private UserRepository users;

    private User owner;

    @BeforeEach
    void seed() {
        reminders.deleteAll();
        users.deleteAll();
        owner = users.save(new User("owner@example.com", "hash", "Owner"));
    }

    private Reminder due(String title, Recurrence recurrence) {
        Reminder reminder = new Reminder(owner, title, "body", Instant.now().minus(5, ChronoUnit.MINUTES));
        reminder.setRecurrence(recurrence);
        return reminders.save(reminder);
    }

    @Test
    void oneOffReminderIsMarkedNotified() {
        Long id = due("pay rent", Recurrence.NONE).getId();

        assertThat(dispatcher.runRound()).isEqualTo(1);

        Reminder after = reminders.findById(id).orElseThrow();
        assertThat(after.getNotifiedAt()).isNotNull();
        assertThat(after.getNotificationAttempts()).isEqualTo(1);
        assertThat(after.getClaimedBy()).isNull();
    }

    @Test
    void recurringReminderRollsForwardInsteadOfBeingRetired() {
        Reminder created = due("standup", Recurrence.DAILY);
        Instant originalDue = created.getDueAt();

        dispatcher.runRound();

        Reminder after = reminders.findById(created.getId()).orElseThrow();
        assertThat(after.getNotifiedAt()).as("still pending for its next occurrence").isNull();
        assertThat(after.getDueAt()).isEqualTo(originalDue.plus(1, ChronoUnit.DAYS));
    }

    @Test
    void alreadyNotifiedRemindersAreNotSentAgain() {
        due("pay rent", Recurrence.NONE);

        assertThat(dispatcher.runRound()).isEqualTo(1);
        assertThat(dispatcher.runRound()).as("second round finds nothing left").isZero();
    }

    @Test
    void remindersThatAreNotDueYetAreLeftAlone() {
        Reminder future = new Reminder(owner, "later", null, Instant.now().plus(1, ChronoUnit.HOURS));
        reminders.save(future);

        assertThat(dispatcher.runRound()).isZero();
    }
}
