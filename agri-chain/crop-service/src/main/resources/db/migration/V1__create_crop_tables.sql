-- Crop Service schema
-- V1: Create crop_listings and orders tables

CREATE TABLE IF NOT EXISTS crop_listings (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    farmer_id        UUID           NOT NULL,
    crop_type        VARCHAR(100)   NOT NULL,
    quantity         DECIMAL(12, 2) NOT NULL CHECK (quantity > 0),
    price_per_unit   DECIMAL(12, 2) NOT NULL CHECK (price_per_unit > 0),
    location         VARCHAR(255)   NOT NULL,
    status           VARCHAR(30)    NOT NULL DEFAULT 'Pending_Approval',
    rejection_reason TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS orders (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id  UUID           NOT NULL REFERENCES crop_listings(id),
    trader_id   UUID           NOT NULL,
    quantity    DECIMAL(12, 2) NOT NULL CHECK (quantity > 0),
    status      VARCHAR(20)    NOT NULL DEFAULT 'Pending',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_crop_listings_farmer_id ON crop_listings(farmer_id);
CREATE INDEX IF NOT EXISTS idx_crop_listings_status    ON crop_listings(status);
CREATE INDEX IF NOT EXISTS idx_orders_listing_id       ON orders(listing_id);
CREATE INDEX IF NOT EXISTS idx_orders_trader_id        ON orders(trader_id);
