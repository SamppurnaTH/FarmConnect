-- Reporting Service schema
-- V1: Create report_metadata table

CREATE TABLE IF NOT EXISTS report_metadata (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    scope             VARCHAR(255) NOT NULL,
    generated_by      UUID         NOT NULL,
    generation_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    date_range_start  DATE,
    date_range_end    DATE,
    format            VARCHAR(10)  NOT NULL DEFAULT 'PDF',
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_report_metadata_generated_by ON report_metadata(generated_by);
