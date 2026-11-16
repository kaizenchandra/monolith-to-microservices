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
create table checkout
(
    id           varchar(100) primary key,
    customer_id  bigint      not null,
    product_id   bigint      not null,
    quantity     integer     not null check (quantity > 0),
    amount_cents bigint      not null check (amount_cents > 0),
    state        text        not null,
    attempts     integer     not null default 0,
    updated_at   timestamptz not null default now()
);
