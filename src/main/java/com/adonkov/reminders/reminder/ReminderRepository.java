package com.adonkov.reminders.reminder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long>, JpaSpecificationExecutor<Reminder> {

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    /**
     * Grabs a batch of reminders that are ready to go out, locking each row so that
     * concurrent workers -- threads here, or other instances of the app -- never pick up
     * the same one. {@code skip locked} is the important part: instead of queueing behind
     * rows another worker already holds, this transaction walks straight past them, so
     * throughput scales with the number of workers instead of serialising on the hottest rows.
     * <p>
     * Written as native SQL because {@code skip locked} isn't expressible through JPA's
     * {@code @Lock} pessimistic modes.
     * <p>
     * Rows claimed by a worker that died are picked up again once the claim goes stale.
     */
    @Query(value = """
            select * from reminders
            where completed = false
              and notified_at is null
              and due_at <= :now
              and notification_attempts < :maxAttempts
              and (claimed_at is null or claimed_at < :staleBefore)
            order by due_at
            limit :batchSize
            for update skip locked
            """, nativeQuery = true)
    List<Reminder> lockDueBatch(@Param("now") Instant now,
                                @Param("staleBefore") Instant staleBefore,
                                @Param("maxAttempts") int maxAttempts,
                                @Param("batchSize") int batchSize);
}
