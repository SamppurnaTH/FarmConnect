-- Transaction Service schema
-- V1: Create transactions and payments tables

CREATE TABLE IF NOT EXISTS transactions (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID           NOT NULL,
    amount      DECIMAL(14, 2) NOT NULL,
    status      VARCHAR(20)    NOT NULL DEFAULT 'Pending_Payment',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,   -- created_at + 48h
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_transactions_order_id UNIQUE (order_id)
);

CREATE TABLE IF NOT EXISTS payments (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID         NOT NULL REFERENCES transactions(id),
    method         VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'Processing',
    gateway_ref    VARCHAR(255),
    failure_reason TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_transactions_order_id   ON transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status     ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_expires_at ON transactions(expires_at);
CREATE INDEX IF NOT EXISTS idx_payments_transaction_id ON payments(transaction_id);
