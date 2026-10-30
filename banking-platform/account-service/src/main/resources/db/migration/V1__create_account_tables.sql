CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    customer_reference VARCHAR(100) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED')),
    currency VARCHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_accounts_customer_reference
    ON accounts (customer_reference);

CREATE TABLE account_balance_operations (
    operation_reference UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts (id),
    operation_type VARCHAR(16) NOT NULL CHECK (operation_type IN ('CREDIT', 'DEBIT')),
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    resulting_balance NUMERIC(19, 4) NOT NULL CHECK (resulting_balance >= 0),
    created_at TIMESTAMPTZ NOT NULL
);