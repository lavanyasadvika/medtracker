-- ═══════════════════════════════════════════════════════════════════════════
-- MedTracker – V3 Schema Alignment
--
-- Aligns the database schema with the JPA entities:
--   1. users  : rename name → full_name, add ADMIN/USER/PHARMACIST/VIEWER to user_role ENUM
--   2. medicines: preserve required owner column user_id from the DDL, rename columns
--                 to match entity field names, change quantity to NUMERIC, add missing
--                 columns (manufacturer, low_stock_threshold, source, created_by,
--                 updated_by), drop columns not in entity
-- ═══════════════════════════════════════════════════════════════════════════

-- ───────────────────────────────────────────────────────────────────────────
-- SECTION 1: Extend user_role ENUM with PHARMACIST and VIEWER
-- PostgreSQL requires ALTER TYPE to add new enum values.
-- ───────────────────────────────────────────────────────────────────────────
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum
        WHERE enumlabel = 'PHARMACIST'
          AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'user_role')
    ) THEN
        ALTER TYPE user_role ADD VALUE 'PHARMACIST';
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum
        WHERE enumlabel = 'VIEWER'
          AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'user_role')
    ) THEN
        ALTER TYPE user_role ADD VALUE 'VIEWER';
    END IF;
END $$;

-- ───────────────────────────────────────────────────────────────────────────
-- SECTION 2: users table – rename name → full_name
-- Entity field: fullName mapped to column full_name (VARCHAR 100)
-- ───────────────────────────────────────────────────────────────────────────
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'name'
    ) THEN
        ALTER TABLE users RENAME COLUMN name TO full_name;
        -- Update the existing check constraint that referenced name
        ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_name_len;
        ALTER TABLE users ADD CONSTRAINT chk_users_name_len
            CHECK (char_length(full_name) >= 2);
    END IF;
END $$;

-- Drop created_by from users (not in User entity)
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'created_by'
    ) THEN
        ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_created_by;
        ALTER TABLE users DROP COLUMN created_by;
    END IF;
END $$;

-- ───────────────────────────────────────────────────────────────────────────
-- SECTION 3: medicines table – add source ENUM type
-- Entity field: source (Source enum: BARCODE, GROQ, MANUAL)
-- ───────────────────────────────────────────────────────────────────────────
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'medicine_source') THEN
        CREATE TYPE medicine_source AS ENUM ('BARCODE', 'GROQ', 'MANUAL');
    END IF;
END $$;

-- ───────────────────────────────────────────────────────────────────────────
-- SECTION 4: medicines table – rename columns to match entity
-- V1 column        →  Entity @Column(name=...)
--   name           →  drug_name
--   brand          →  (drop – not in entity)
--   barcode        →  barcode_data
--   purchase_price →  price
-- ───────────────────────────────────────────────────────────────────────────

-- 4a. Rename name → drug_name
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'medicines' AND column_name = 'name'
    ) THEN
        ALTER TABLE medicines RENAME COLUMN name TO drug_name;
        -- Adjust length to match entity (200)
        ALTER TABLE medicines ALTER COLUMN drug_name TYPE VARCHAR(200);
        -- Recreate name-length check on new column name
        ALTER TABLE medicines DROP CONSTRAINT IF EXISTS chk_medicines_name_len;
        ALTER TABLE medicines ADD CONSTRAINT chk_medicines_name_len
            CHECK (char_length(drug_name) >= 1);
    END IF;
END $$;

-- 4b. Rename barcode → barcode_data (add UNIQUE constraint per entity)
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'medicines' AND column_name = 'barcode'
    ) THEN
        ALTER TABLE medicines RENAME COLUMN barcode TO barcode_data;
        ALTER TABLE medicines ADD CONSTRAINT uq_medicines_barcode_data
            UNIQUE (barcode_data);
    END IF;
END $$;

-- 4c. Rename purchase_price → price
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'medicines' AND column_name = 'purchase_price'
    ) THEN
        ALTER TABLE medicines RENAME COLUMN purchase_price TO price;
        -- Recreate price check constraint
        ALTER TABLE medicines DROP CONSTRAINT IF EXISTS chk_medicines_price;
        ALTER TABLE medicines ADD CONSTRAINT chk_medicines_price
            CHECK (price IS NULL OR price >= 0);
    END IF;
END $$;

-- ───────────────────────────────────────────────────────────────────────────
-- SECTION 5: medicines table – fix quantity type INTEGER → NUMERIC(10,2)
-- Entity field: quantity BigDecimal → NUMERIC(10,2)
-- ───────────────────────────────────────────────────────────────────────────
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'medicines'
          AND column_name = 'quantity'
          AND data_type = 'integer'
    ) THEN
        -- Drop old check before altering type
        ALTER TABLE medicines DROP CONSTRAINT IF EXISTS chk_medicines_qty;
        ALTER TABLE medicines ALTER COLUMN quantity TYPE NUMERIC(10, 2)
            USING quantity::NUMERIC(10, 2);
        ALTER TABLE medicines ADD CONSTRAINT chk_medicines_qty
            CHECK (quantity >= 0);
    END IF;
END $$;

-- ───────────────────────────────────────────────────────────────────────────
-- SECTION 6: medicines table – add missing columns from entity
-- manufacturer, low_stock_threshold, source, created_by, updated_by
-- ───────────────────────────────────────────────────────────────────────────

-- 6a. manufacturer (replaces brand in entity)
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'medicines' AND column_name = 'manufacturer'
    ) THEN
        ALTER TABLE medicines ADD COLUMN manufacturer VARCHAR(100);
    END IF;
END $$;

-- 6b. low_stock_threshold
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'medicines' AND column_name = 'low_stock_threshold'
    ) THEN
        ALTER TABLE medicines ADD COLUMN low_stock_threshold NUMERIC(10, 2);
    END IF;
END $$;

-- 6c. source (medicine_source ENUM)
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'medicines' AND column_name = 'source'
    ) THEN
        -- Add with a temporary default so existing rows get a value
        ALTER TABLE medicines
            ADD COLUMN source medicine_source NOT NULL DEFAULT 'MANUAL';
        -- Remove the default – future inserts must supply source explicitly
        ALTER TABLE medicines ALTER COLUMN source DROP DEFAULT;
    END IF;
END $$;

-- 6d. created_by (audit: who created this record)
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'medicines' AND column_name = 'created_by'
    ) THEN
        -- Nullable initially; existing rows have no creator info
        ALTER TABLE medicines ADD COLUMN created_by BIGINT;
        ALTER TABLE medicines ADD CONSTRAINT fk_medicines_created_by
            FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL;
    END IF;
END $$;

-- 6e. updated_by (audit: who last updated this record)
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'medicines' AND column_name = 'updated_by'
    ) THEN
        ALTER TABLE medicines ADD COLUMN updated_by BIGINT;
        ALTER TABLE medicines ADD CONSTRAINT fk_medicines_updated_by
            FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL;
    END IF;
END $$;

-- ───────────────────────────────────────────────────────────────────────────
-- SECTION 7: medicines table – drop V1 columns not present in entity
-- brand, unit, dosage_form, strength, manufacture_date, purchase_date,
-- storage_location, notes
-- ───────────────────────────────────────────────────────────────────────────

DO $$ DECLARE
    col TEXT;
BEGIN
    FOREACH col IN ARRAY ARRAY[
        'brand', 'unit', 'dosage_form', 'strength',
        'manufacture_date', 'purchase_date', 'storage_location', 'notes'
    ]
    LOOP
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'medicines' AND column_name = col
        ) THEN
            EXECUTE format('ALTER TABLE medicines DROP COLUMN %I', col);
        END IF;
    END LOOP;
END $$;

-- ───────────────────────────────────────────────────────────────────────────
-- SECTION 8: Update indexes to reflect renamed columns
-- ───────────────────────────────────────────────────────────────────────────

-- Drop old barcode index (column renamed to barcode_data)
DROP INDEX IF EXISTS idx_medicines_barcode;

-- Recreate barcode_data index
CREATE INDEX IF NOT EXISTS idx_medicines_barcode_data
    ON medicines (barcode_data)
    WHERE barcode_data IS NOT NULL;

-- Index on source for quick filtering
CREATE INDEX IF NOT EXISTS idx_medicines_source
    ON medicines (source);

-- Composite index: user_id + source (common query pattern)
CREATE INDEX IF NOT EXISTS idx_medicines_user_source
    ON medicines (user_id, source);

COMMENT ON COLUMN medicines.drug_name         IS 'Generic or brand name of the medicine';
COMMENT ON COLUMN medicines.manufacturer      IS 'Manufacturer / supplier name';
COMMENT ON COLUMN medicines.barcode_data      IS 'Scanned or manually entered barcode / QR value (unique)';
COMMENT ON COLUMN medicines.quantity          IS 'Current stock quantity; supports fractional amounts (NUMERIC)';
COMMENT ON COLUMN medicines.low_stock_threshold IS 'Alert threshold; null means no alert';
COMMENT ON COLUMN medicines.source            IS 'How the medicine record was created: BARCODE, GROQ, or MANUAL';
COMMENT ON COLUMN medicines.price             IS 'Purchase price per unit';
COMMENT ON COLUMN medicines.created_by        IS 'FK to users.id – who created this record';
COMMENT ON COLUMN medicines.updated_by        IS 'FK to users.id – who last updated this record';
