alter table reminders
    add column version               bigint      not null default 0,
    add column recurrence            varchar(20) not null default 'NONE',
    add column notified_at           timestamptz,
    add column notification_attempts integer     not null default 0,
    add column claimed_at            timestamptz,
    add column claimed_by            varchar(100);

-- the dispatcher only ever scans for reminders that are still pending,
-- so a partial index keeps it small no matter how much history piles up
create index idx_reminders_pending_notification
    on reminders (due_at)
    where completed = false and notified_at is null;
