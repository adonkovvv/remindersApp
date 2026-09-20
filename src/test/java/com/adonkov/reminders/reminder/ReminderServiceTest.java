package com.adonkov.reminders.reminder;

import com.adonkov.reminders.notification.NotificationProperties;
import com.adonkov.reminders.user.User;
import com.adonkov.reminders.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private ReminderRepository repository;

    @Mock
    private UserRepository users;

    private ReminderService service;
    private Reminder existing;
    private Instant originalDue;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties(
                true,
                Duration.ofSeconds(30),
                Duration.ofSeconds(15),
                100,
                4,
                500,
                5,
                Duration.ofMinutes(10),
                Duration.ofMinutes(5));

        service = new ReminderService(repository, users, properties);

        originalDue = Instant.now().plus(1, ChronoUnit.DAYS);
        existing = new Reminder(new User("ivan@example.com", "hash", "Ivan"), "pay rent", "before the 5th", originalDue);
    }

    @Test
    void snoozeWithoutABodyUsesTheConfiguredDefault() {
        given(repository.findByIdAndUserId(1L, USER_ID)).willReturn(Optional.of(existing));

        service.snooze(USER_ID, 1L, null);

        assertThat(existing.getDueAt()).isEqualTo(originalDue.plus(10, ChronoUnit.MINUTES));
    }

    @Test
    void snoozeHonoursAnExplicitDuration() {
        given(repository.findByIdAndUserId(1L, USER_ID)).willReturn(Optional.of(existing));

        service.snooze(USER_ID, 1L, new ReminderDtos.SnoozeRequest(Duration.ofHours(2)));

        assertThat(existing.getDueAt()).isEqualTo(originalDue.plus(2, ChronoUnit.HOURS));
    }

    @Test
    void snoozeRejectsANonPositiveDuration() {
        assertThatThrownBy(() ->
                service.snooze(USER_ID, 1L, new ReminderDtos.SnoozeRequest(Duration.ofMinutes(-5))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateOnlyTouchesTheFieldsThatWereSent() {
        given(repository.findByIdAndUserId(1L, USER_ID)).willReturn(Optional.of(existing));

        service.update(USER_ID, 1L, new ReminderDtos.UpdateRequest(
                "pay rent (urgent)", null, null, null, null));

        assertThat(existing.getTitle()).isEqualTo("pay rent (urgent)");
        assertThat(existing.getDescription()).isEqualTo("before the 5th");
        assertThat(existing.getDueAt()).isEqualTo(originalDue);
        assertThat(existing.isCompleted()).isFalse();
    }

    @Test
    void anotherUsersReminderLooksMissing() {
        given(repository.findByIdAndUserId(1L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(USER_ID, 1L))
                .isInstanceOf(ReminderNotFoundException.class);
    }
}
