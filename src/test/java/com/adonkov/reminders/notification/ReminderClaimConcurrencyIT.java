package com.adonkov.reminders.notification;

import com.adonkov.reminders.AbstractPostgresTest;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.notifications.batch-size=10",
        "app.notifications.claim-timeout=5m"
})
class ReminderClaimConcurrencyIT extends AbstractPostgresTest {

    private static final int REMINDERS = 120;
    private static final int THREADS = 8;

    @Autowired
    private ReminderClaimService claimService;

    @Autowired
    private ReminderRepository reminders;

    @Autowired
    private UserRepository users;

    @BeforeEach
    void seed() {
        reminders.deleteAll();
        users.deleteAll();

        User owner = users.save(new User("racer@example.com", "hash", "Racer"));
        Instant overdue = Instant.now().minus(1, ChronoUnit.HOURS);

        List<Reminder> due = IntStream.range(0, REMINDERS)
                .mapToObj(i -> new Reminder(owner, "reminder " + i, null, overdue))
                .toList();

        reminders.saveAll(due);
    }

    /**
     * The whole point of {@code for update skip locked}: several workers hammering the
     * same queue must partition it, never hand the same reminder to two of them.
     */
    @Test
    void concurrentWorkersNeverClaimTheSameReminderTwice() throws Exception {
        List<Long> claimed = Collections.synchronizedList(new ArrayList<>());

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);

        try {
            for (int worker = 0; worker < THREADS; worker++) {
                String instanceId = "worker-" + worker;
                pool.submit(() -> {
                    try {
                        start.await();
                        // keep draining until this worker finds nothing left
                        List<Notification> batch;
                        do {
                            batch = claimService.claimBatch(instanceId);
                            batch.forEach(n -> claimed.add(n.reminderId()));
                        } while (!batch.isEmpty());
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        Set<Long> distinct = Set.copyOf(claimed);

        assertThat(claimed)
                .as("no reminder may be claimed twice")
                .hasSize(distinct.size());

        assertThat(distinct)
                .as("every due reminder gets claimed exactly once")
                .hasSize(REMINDERS);
    }

    @Test
    void claimedRemindersAreStampedWithTheClaimingInstance() {
        List<Notification> batch = claimService.claimBatch("worker-alpha");

        assertThat(batch).isNotEmpty();

        Set<Long> ids = batch.stream().map(Notification::reminderId).collect(Collectors.toSet());

        assertThat(reminders.findAllById(ids))
                .allSatisfy(reminder -> {
                    assertThat(reminder.getClaimedBy()).isEqualTo("worker-alpha");
                    assertThat(reminder.getClaimedAt()).isNotNull();
                });
    }
}
