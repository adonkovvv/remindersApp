package com.adonkov.reminders.reminder;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceTest {

    private static Instant utc(String isoDateTime) {
        return Instant.parse(isoDateTime);
    }

    @Test
    void noneHasNoNextOccurrence() {
        assertThat(Recurrence.NONE.nextAfter(Instant.now())).isNull();
        assertThat(Recurrence.NONE.repeats()).isFalse();
    }

    @Test
    void dailyAddsADay() {
        Instant from = utc("2026-03-10T09:00:00Z");
        assertThat(Recurrence.DAILY.nextAfter(from)).isEqualTo(from.plus(1, ChronoUnit.DAYS));
    }

    @Test
    void weeklyAddsSevenDays() {
        Instant from = utc("2026-03-10T09:00:00Z");
        assertThat(Recurrence.WEEKLY.nextAfter(from)).isEqualTo(utc("2026-03-17T09:00:00Z"));
    }

    @Test
    void monthlyKeepsTheDayOfMonth() {
        assertThat(Recurrence.MONTHLY.nextAfter(utc("2026-03-10T09:00:00Z")))
                .isEqualTo(utc("2026-04-10T09:00:00Z"));
    }

    @Test
    void monthlyClampsWhenTheTargetMonthIsShorter() {
        // 31 Jan + 1 month must land on 28 Feb, not spill into March
        assertThat(Recurrence.MONTHLY.nextAfter(utc("2026-01-31T09:00:00Z")))
                .isEqualTo(utc("2026-02-28T09:00:00Z"));
    }

    @Test
    void monthlyHandlesLeapYears() {
        assertThat(Recurrence.MONTHLY.nextAfter(utc("2028-01-31T09:00:00Z")))
                .isEqualTo(utc("2028-02-29T09:00:00Z"));
    }

    @Test
    void everythingButNoneRepeats() {
        assertThat(Recurrence.DAILY.repeats()).isTrue();
        assertThat(Recurrence.WEEKLY.repeats()).isTrue();
        assertThat(Recurrence.MONTHLY.repeats()).isTrue();
    }
}
