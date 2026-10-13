-- Armazena a conexão do usuário com cada banco via Pluggy
CREATE TABLE pluggy_connections (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id),
    item_id     VARCHAR(100) NOT NULL,  -- ID do item na Pluggy
    connector_name VARCHAR(100),        -- Nome do banco (Nubank, Itaú...)
    connector_id   INTEGER,             -- ID do conector Pluggy
    status      VARCHAR(20)  NOT NULL DEFAULT 'UPDATED',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Vincula contas do sistema com contas da Pluggy
ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS pluggy_account_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS pluggy_item_id    VARCHAR(100);

-- Evita importar a mesma transação duas vezes
ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS pluggy_transaction_id VARCHAR(100) UNIQUE;