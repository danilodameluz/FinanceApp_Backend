ALTER TABLE categories
    DROP CONSTRAINT categories_type_check;

ALTER TABLE categories
    ADD CONSTRAINT categories_type_check
    CHECK (type IN ('INCOME', 'EXPENSE'));

ALTER TABLE transactions
    DROP CONSTRAINT transactions_type_check;

ALTER TABLE transactions
    ADD CONSTRAINT transactions_type_check
    CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER'));