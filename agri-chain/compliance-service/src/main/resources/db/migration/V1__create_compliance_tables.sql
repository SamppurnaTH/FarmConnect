-- Compliance Service schema
-- V1: Create compliance_records and audits tables

CREATE TABLE IF NOT EXISTS compliance_records (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type  VARCHAR(100) NOT NULL,
    entity_id    UUID         NOT NULL,
    check_result VARCHAR(10)  NOT NULL,
    check_date   DATE         NOT NULL,
    notes        TEXT,
    created_by   UUID         NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS audits (
    id           UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    scope        TEXT    NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'In_Progress',
    findings     TEXT,
    initiated_by UUID    NOT NULL,
    initiated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_compliance_records_entity ON compliance_records(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audits_status             ON audits(status);
CREATE INDEX IF NOT EXISTS idx_audits_initiated_by       ON audits(initiated_by);
