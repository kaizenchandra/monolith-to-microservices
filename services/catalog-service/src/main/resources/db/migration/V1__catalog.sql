create table ownership
(
    capability varchar(32) primary key,
    state      varchar(16) not null check (state in ('LEGACY', 'FROZEN', 'MODERN')),
    epoch      bigint      not null default 0
);
create table outbox
(
    id           uuid primary key,
    aggregate_id varchar(100) not null,
    payload      text         not null,
    published    boolean      not null default false,
    created_at   timestamptz  not null default now()
);
create table products
(
    id          bigint primary key,
    name        text    not null,
    price_cents bigint  not null check (price_cents >= 0),
    version     bigint  not null,
    deleted     boolean not null default false
);
insert into ownership
values ('catalog', 'FROZEN', 0);
create table inbox
(
    event_id uuid primary key
);
