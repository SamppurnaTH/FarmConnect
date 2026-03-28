-- Notification Service schema
-- V1: Create notifications table

CREATE TABLE IF NOT EXISTS notifications (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL,
    channel      VARCHAR(20) NOT NULL,
    message      TEXT        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'Pending',
    retry_count  INT         NOT NULL DEFAULT 0,
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at      TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status  ON notifications(status);
