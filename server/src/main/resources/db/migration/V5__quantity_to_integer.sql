-- ═══════════════════════════════════════════════════════════════════════════
-- MedTracker – V5 Quantity type revert
--
-- Reverts medicines.quantity and medicines.low_stock_threshold from
-- NUMERIC(10,2) (introduced in V3 to "support fractional amounts") back to
-- INTEGER. Fractional stock was never an actual product requirement, and
-- BigDecimal added arithmetic/validation overhead with no real benefit.
-- Existing fractional values, if any, are rounded to the nearest whole unit.
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE medicines DROP CONSTRAINT IF EXISTS chk_medicines_qty;

ALTER TABLE medicines ALTER COLUMN quantity TYPE INTEGER
    USING ROUND(quantity)::INTEGER;

ALTER TABLE medicines ALTER COLUMN low_stock_threshold TYPE INTEGER
    USING ROUND(low_stock_threshold)::INTEGER;

ALTER TABLE medicines ADD CONSTRAINT chk_medicines_qty
    CHECK (quantity >= 0);

COMMENT ON COLUMN medicines.quantity            IS 'Current stock quantity (whole units)';
COMMENT ON COLUMN medicines.low_stock_threshold IS 'Alert threshold (whole units); null means no alert';
