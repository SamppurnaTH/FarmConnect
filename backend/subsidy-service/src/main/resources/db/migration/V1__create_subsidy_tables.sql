-- Subsidy Service schema
-- V1: Create subsidy_programs and disbursements tables

CREATE TABLE IF NOT EXISTS subsidy_programs (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(255)   NOT NULL,
    description      TEXT,
    start_date       DATE           NOT NULL,
    end_date         DATE           NOT NULL,
    budget_amount    DECIMAL(16, 2) NOT NULL,
    total_disbursed  DECIMAL(16, 2) NOT NULL DEFAULT 0,
    status           VARCHAR(20)    NOT NULL DEFAULT 'Draft',
    created_by       UUID           NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS disbursements (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id     UUID           NOT NULL REFERENCES subsidy_programs(id),
    farmer_id      UUID           NOT NULL,
    amount         DECIMAL(14, 2) NOT NULL,
    status         VARCHAR(20)    NOT NULL DEFAULT 'Pending',
    approved_by    UUID,
    approved_at    TIMESTAMP WITH TIME ZONE,
    program_cycle  VARCHAR(50)    NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_disbursement_farmer_program_cycle UNIQUE (farmer_id, program_id, program_cycle)
);

CREATE INDEX IF NOT EXISTS idx_subsidy_programs_status ON subsidy_programs(status);
CREATE INDEX IF NOT EXISTS idx_disbursements_program   ON disbursements(program_id);
CREATE INDEX IF NOT EXISTS idx_disbursements_farmer    ON disbursements(farmer_id);
