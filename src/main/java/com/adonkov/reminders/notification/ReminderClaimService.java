package com.adonkov.reminders.notification;

import com.adonkov.reminders.reminder.Reminder;
import com.adonkov.reminders.reminder.ReminderRepository;
import com.adonkov.reminders.user.User;
import com.adonkov.reminders.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Owns the short transactions around a dispatch round. Claiming and recording the
 * outcome are separate units of work so that the row locks are released before the
 * slow part -- actually sending -- begins.
 */
@Service
public class ReminderClaimService {

    private static final Logger log = LoggerFactory.getLogger(ReminderClaimService.class);

    private final ReminderRepository reminders;
    private final UserRepository users;
    private final NotificationProperties properties;

    public ReminderClaimService(ReminderRepository reminders,
                                UserRepository users,
                                NotificationProperties properties) {
        this.reminders = reminders;
        this.users = users;
        this.properties = properties;
    }

    @Transactional
    public List<Notification> claimBatch(String instanceId) {
        Instant now = Instant.now();
        Instant staleBefore = now.minus(properties.claimTimeout());

        List<Reminder> batch = reminders.lockDueBatch(
                now, staleBefore, properties.maxAttempts(), properties.batchSize());

        if (batch.isEmpty()) {
            return List.of();
        }

        // one query for the addresses rather than a lazy load per reminder
        Map<Long, String> emailsByUserId = users.findAllById(
                        batch.stream().map(r -> r.getUser().getId()).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));

        batch.forEach(reminder -> reminder.claim(instanceId, now));

        log.debug("claimed {} reminder(s) as {}", batch.size(), instanceId);

        return batch.stream()
                .map(r -> new Notification(
                        r.getId(),
                        r.getUser().getId(),
                        emailsByUserId.get(r.getUser().getId()),
                        r.getTitle(),
                        r.getDescription(),
                        r.getDueAt()))
                .toList();
    }

    /**
     * Runs in its own transaction so one reminder's outcome can't roll back the rest of the batch.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDelivered(Long reminderId) {
        reminders.findById(reminderId).ifPresent(r -> r.markNotified(Instant.now()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long reminderId) {
        reminders.findById(reminderId).ifPresent(Reminder::markNotificationFailed);
    }
}
