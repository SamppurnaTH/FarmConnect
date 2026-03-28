CREATE TABLE IF NOT EXISTS traders (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL UNIQUE,
    name        VARCHAR(500) NOT NULL,
    organization VARCHAR(500),
    contact_info VARCHAR(500) NOT NULL UNIQUE,
    status      VARCHAR(20) NOT NULL DEFAULT 'Active',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
