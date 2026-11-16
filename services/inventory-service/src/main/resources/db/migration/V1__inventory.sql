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
create table stock
(
    product_id bigint primary key,
    available  integer not null check (available >= 0),
    version    bigint  not null default 1
);
create table reservations
(
    id         varchar(100) primary key,
    product_id bigint      not null references stock (product_id),
    quantity   integer     not null check (quantity > 0),
    state      varchar(16) not null check (state in ('RESERVED', 'RELEASED'))
);
insert into ownership
values ('inventory', 'FROZEN', 0);
