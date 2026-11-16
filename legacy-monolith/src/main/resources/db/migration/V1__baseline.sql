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
values ('catalog', 'LEGACY', 0),
       ('inventory', 'LEGACY', 0),
       ('checkout', 'LEGACY', 0);
insert into products
values (1, 'Mechanical Keyboard', 12999, 1, false),
       (2, 'USB Cable', 1499, 1, false);
insert into stock
values (1, 10, 1),
       (2, 100, 1);
create table customers
(
    id   bigint primary key,
    name text not null
);
insert into customers
values (1, 'Demo Customer');
create table orders
(
    id           varchar(100) primary key,
    customer_id  bigint  not null references customers (id),
    product_id   bigint  not null,
    quantity     integer not null,
    amount_cents bigint  not null,
    state        text    not null
);
create table payments
(
    order_id     varchar(100) primary key,
    amount_cents bigint not null,
    state        text   not null
);
create table shipments
(
    order_id varchar(100) primary key,
    state    text not null
);
create table notifications
(
    order_id varchar(100) primary key,
    message  text not null
);
create table routing
(
    id      integer primary key check (id = 1),
    mode    text    not null check (mode in ('LEGACY', 'SHADOW', 'CANARY', 'MODERN')),
    percent integer not null check (percent between 0 and 100)
);
insert into routing
values (1, 'LEGACY', 0);
