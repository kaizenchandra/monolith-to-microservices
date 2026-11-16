create table inbox
(
    event_id uuid primary key
);
create table modern_orders
(
    id           varchar(100) primary key,
    amount_cents bigint not null
);
