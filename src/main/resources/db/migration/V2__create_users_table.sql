create table users (
    id            bigserial primary key,
    email         varchar(255) not null,
    password_hash varchar(100) not null,
    display_name  varchar(100),
    created_at    timestamptz  not null default now()
);

create unique index idx_users_email on users (lower(email));
