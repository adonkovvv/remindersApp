package com.adonkov.reminders.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scans for reminders that have come due and hands them to the worker pool.
 * <p>
 * A round is three phases, deliberately kept apart:
 * <ol>
 *   <li>claim a bounded batch inside one short transaction (row locks held here only),</li>
 *   <li>send them concurrently on {@code notificationExecutor}, holding no locks,</li>
 *   <li>record each outcome in its own transaction.</li>
 * </ol>
 * Sending inside the claiming transaction would hold row locks for the length of a
 * network call, which is what makes naive versions of this fall over under load.
 */
@Component
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ReminderDispatcher.class);

    private final ReminderClaimService claims;
    private final NotificationSender sender;
    private final ThreadPoolTaskExecutor executor;
    private final String instanceId;

    private final AtomicLong delivered = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    public ReminderDispatcher(ReminderClaimService claims,
                              NotificationSender sender,
                              @Qualifier("notificationExecutor") ThreadPoolTaskExecutor executor) {
        this.claims = claims;
        this.sender = sender;
        this.executor = executor;
        this.instanceId = buildInstanceId();
    }

    /**
     * {@code fixedDelay} measures from the end of the previous run, so rounds can't overlap
     * even when one takes longer than the interval.
     */
    @Scheduled(fixedDelayString = "${app.notifications.scan-interval}")
    public void dispatchDue() {
        try {
            int sent = runRound();
            if (sent > 0) {
                log.info("dispatched {} reminder(s) using {}", sent, sender.name());
            }
        } catch (RuntimeException ex) {
            // never let a bad round kill the schedule
            log.error("dispatch round failed", ex);
        }
    }

    int runRound() {
        List<Notification> batch = claims.claimBatch(instanceId);
        if (batch.isEmpty()) {
            return 0;
        }

        CompletableFuture<?>[] inFlight = batch.stream()
                .map(notification -> CompletableFuture.runAsync(() -> deliver(notification), executor))
                .toArray(CompletableFuture[]::new);

        try {
            // every task swallows its own failure, so this only waits
            CompletableFuture.allOf(inFlight).join();
        } catch (CompletionException ex) {
            log.warn("a delivery task ended unexpectedly", ex.getCause());
        }

        return batch.size();
    }

    private void deliver(Notification notification) {
        try {
            sender.send(notification);
            claims.recordDelivered(notification.reminderId());
            delivered.incrementAndGet();
        } catch (RuntimeException ex) {
            log.warn("could not deliver reminder {}: {}", notification.reminderId(), ex.toString());
            claims.recordFailure(notification.reminderId());
            failed.incrementAndGet();
        }
    }

    public long deliveredCount() {
        return delivered.get();
    }

    public long failedCount() {
        return failed.get();
    }

    public String instanceId() {
        return instanceId;
    }

    /**
     * Identifies this process in the {@code claimed_by} column, which is what makes a
     * stuck claim traceable back to the instance that took it.
     */
    private static String buildInstanceId() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            host = "unknown";
        }
        return host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
