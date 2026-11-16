SET LOCAL lock_timeout = '3s';
-- Sandbox zero authorization has no external charge. Real adapters must skip payment.
ALTER TABLE authorizations DROP CONSTRAINT authorizations_amount_cents_check;
ALTER TABLE authorizations
    ADD CONSTRAINT authorizations_amount_cents_check CHECK (amount_cents >= 0);
