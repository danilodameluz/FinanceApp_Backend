ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS destination_account_id BIGINT REFERENCES accounts(id);