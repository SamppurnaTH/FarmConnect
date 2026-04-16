-- V2: Add rejection_reason column to disbursements table
ALTER TABLE disbursements ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
