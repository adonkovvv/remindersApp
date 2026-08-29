alter table reminders add column user_id bigint;

-- reminders created before auth existed have no owner to assign
delete from reminders where user_id is null;

alter table reminders alter column user_id set not null;

alter table reminders
    add constraint fk_reminders_user foreign key (user_id) references users (id) on delete cascade;

drop index idx_reminders_due_at;
create index idx_reminders_user_due on reminders (user_id, due_at);
