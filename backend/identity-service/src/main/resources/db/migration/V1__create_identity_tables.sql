-- Identity Service schema
-- V1: Create users and audit_log tables

CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    email           VARCHAR(500) NOT NULL,   -- AES-256 encrypted
    role            VARCHAR(50)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'Active',
    failed_attempts INT          NOT NULL DEFAULT 0,
    locked_at       TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email)
);

-- Append-only audit log.
-- Enforce immutability via a rule that prevents UPDATE and DELETE.
CREATE TABLE IF NOT EXISTS audit_log (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL REFERENCES users(id),
    action_type    VARCHAR(50)  NOT NULL,
    resource_type  VARCHAR(100) NOT NULL,
    resource_id    UUID         NOT NULL,
    previous_value JSONB,
    new_value      JSONB,
    timestamp      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Prevent any UPDATE on audit_log (append-only enforcement)
CREATE RULE no_update_audit_log AS ON UPDATE TO audit_log DO INSTEAD NOTHING;

-- Prevent any DELETE on audit_log (append-only enforcement)
CREATE RULE no_delete_audit_log AS ON DELETE TO audit_log DO INSTEAD NOTHING;

-- Index for common query patterns
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id       ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_resource      ON audit_log(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp     ON audit_log(timestamp DESC);
