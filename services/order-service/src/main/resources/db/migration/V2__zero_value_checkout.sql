SET LOCAL lock_timeout = '3s';
-- Catalog permits zero-priced products; preserve legacy free-order semantics.
ALTER TABLE checkout DROP CONSTRAINT checkout_amount_cents_check;
ALTER TABLE checkout
    ADD CONSTRAINT checkout_amount_cents_check CHECK (amount_cents >= 0);
