create table reminders (
    id          bigserial primary key,
    title       varchar(200) not null,
    description text,
    due_at      timestamptz  not null,
    completed   boolean      not null default false,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now()
);

create index idx_reminders_due_at on reminders (due_at);
