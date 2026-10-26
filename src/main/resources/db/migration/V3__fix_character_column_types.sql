ALTER TABLE accounts
ALTER COLUMN currency TYPE VARCHAR(3)
    USING TRIM(currency)::VARCHAR(3);

ALTER TABLE transfers
ALTER COLUMN currency TYPE VARCHAR(3)
    USING TRIM(currency)::VARCHAR(3);

ALTER TABLE idempotency_records
ALTER COLUMN request_hash TYPE VARCHAR(64)
    USING TRIM(request_hash)::VARCHAR(64);

ALTER TABLE simulated_settlements
ALTER COLUMN currency TYPE VARCHAR(3)
    USING TRIM(currency)::VARCHAR(3);