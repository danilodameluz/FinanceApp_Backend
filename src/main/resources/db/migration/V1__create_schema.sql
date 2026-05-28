-- Tabela de usuários
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Tabela de contas bancárias
CREATE TABLE accounts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id),
    name       VARCHAR(100) NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    balance    NUMERIC(15,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Tabela de categorias
CREATE TABLE categories (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT      NOT NULL REFERENCES users(id),
    name    VARCHAR(100) NOT NULL,
    type    VARCHAR(10)  NOT NULL CHECK (type IN ('income', 'expense')),
    icon    VARCHAR(50),
    color   VARCHAR(20),
    budget  NUMERIC(15,2) DEFAULT 0
);

-- Tabela de transações
CREATE TABLE transactions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users(id),
    account_id  BIGINT        NOT NULL REFERENCES accounts(id),
    category_id BIGINT        REFERENCES categories(id),
    description VARCHAR(255)  NOT NULL,
    amount      NUMERIC(15,2) NOT NULL,
    type        VARCHAR(10)   NOT NULL CHECK (type IN ('income', 'expense', 'transfer')),
    date        DATE          NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Índices para performance
CREATE INDEX idx_transactions_user_id    ON transactions(user_id);
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_date       ON transactions(date);
CREATE INDEX idx_accounts_user_id        ON accounts(user_id);
CREATE INDEX idx_categories_user_id      ON categories(user_id);