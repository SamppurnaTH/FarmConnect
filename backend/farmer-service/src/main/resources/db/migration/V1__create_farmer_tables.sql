-- Farmer Service schema
-- V1: Create farmers and farmer_documents tables

CREATE TABLE IF NOT EXISTS farmers (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL,
    name          VARCHAR(500) NOT NULL,   -- AES-256 encrypted
    date_of_birth VARCHAR(500) NOT NULL,   -- AES-256 encrypted (stored as text)
    gender        VARCHAR(20),
    address       TEXT         NOT NULL,   -- AES-256 encrypted
    contact_info  VARCHAR(500) NOT NULL,   -- AES-256 encrypted
    land_details  TEXT,
    status        VARCHAR(30)  NOT NULL DEFAULT 'Pending_Verification',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Note: unique constraint on contact_info is enforced at application level
-- because the column stores encrypted values (same plaintext → different ciphertext per encryption).
-- A hash-based unique index can be added in a future migration if deterministic encryption is adopted.

CREATE TABLE IF NOT EXISTS farmer_documents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farmer_id           UUID         NOT NULL REFERENCES farmers(id),
    document_type       VARCHAR(30)  NOT NULL,
    storage_path        VARCHAR(500) NOT NULL,
    verification_status VARCHAR(20)  NOT NULL DEFAULT 'Pending',
    reviewed_by         UUID,
    reviewed_at         TIMESTAMP WITH TIME ZONE,
    rejection_reason    TEXT,
    uploaded_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_farmers_user_id          ON farmers(user_id);
CREATE INDEX IF NOT EXISTS idx_farmer_docs_farmer_id    ON farmer_documents(farmer_id);
CREATE INDEX IF NOT EXISTS idx_farmer_docs_status       ON farmer_documents(verification_status);
