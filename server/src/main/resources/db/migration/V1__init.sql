-- ═══════════════════════════════════════════════════════════════════════════
-- MedTracker – V1 Initial Schema
-- Source of truth: MedTracker_DB_Design_Final.docx
-- ═══════════════════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────────────────
-- 0. Extensions
-- ─────────────────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─────────────────────────────────────────────────────────────────────────
-- 1. ENUM types
-- ─────────────────────────────────────────────────────────────────────────
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        CREATE TYPE user_role AS ENUM ('ADMIN', 'USER');
    END IF;
END $$;

-- ─────────────────────────────────────────────────────────────────────────
-- 2. FUNCTION – auto-update updated_at (Section 5 of DB Design)
-- ─────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION fn_set_updated_at()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

-- ─────────────────────────────────────────────────────────────────────────
-- 3. TABLE: users
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    role            user_role       NOT NULL DEFAULT 'USER',
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_by      BIGINT,                                      -- NULL for bootstrap admin
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────────
    CONSTRAINT uq_users_email        UNIQUE (email),
    CONSTRAINT chk_users_email       CHECK (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    CONSTRAINT chk_users_name_len    CHECK (char_length(name) >= 2),
    CONSTRAINT fk_users_created_by   FOREIGN KEY (created_by)
                                         REFERENCES users (id)
                                         ON DELETE SET NULL
);

COMMENT ON TABLE  users                IS 'Application users – both ADMIN and USER roles';
COMMENT ON COLUMN users.password_hash  IS 'BCrypt-hashed password; never store plain text';
COMMENT ON COLUMN users.created_by     IS 'NULL for the bootstrap admin; FK to users.id for all others';

-- Trigger: keep updated_at current on every UPDATE
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION fn_set_updated_at();

-- ─────────────────────────────────────────────────────────────────────────
-- 4. TABLE: medicines
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS medicines (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    brand               VARCHAR(255),
    category            VARCHAR(100),                            -- Added per agreed spec
    batch_number        VARCHAR(100),
    barcode             VARCHAR(100),
    quantity            INTEGER         NOT NULL DEFAULT 0,
    unit                VARCHAR(50),
    dosage_form         VARCHAR(100),
    strength            VARCHAR(100),
    expiry_date         DATE            NOT NULL,
    manufacture_date    DATE,
    purchase_date       DATE,
    purchase_price      NUMERIC(10, 2),
    storage_location    VARCHAR(255),
    notes               TEXT,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────────
    CONSTRAINT fk_medicines_user        FOREIGN KEY (user_id)
                                            REFERENCES users (id)
                                            ON DELETE CASCADE,
    CONSTRAINT chk_medicines_qty        CHECK (quantity >= 0),
    CONSTRAINT chk_medicines_price      CHECK (purchase_price IS NULL OR purchase_price >= 0),
    CONSTRAINT chk_medicines_name_len   CHECK (char_length(name) >= 1),
    CONSTRAINT chk_medicines_expiry     CHECK (manufacture_date IS NULL
                                             OR expiry_date > manufacture_date)
);

COMMENT ON TABLE  medicines              IS 'Medicine inventory entries owned by a user';
COMMENT ON COLUMN medicines.category     IS 'Optional free-text category (e.g. Antibiotic, Analgesic)';
COMMENT ON COLUMN medicines.barcode      IS 'Scanned or manually entered barcode / QR value';
COMMENT ON COLUMN medicines.dosage_form  IS 'e.g. Tablet, Capsule, Syrup, Injection';

-- Trigger: keep updated_at current on every UPDATE
CREATE TRIGGER trg_medicines_updated_at
    BEFORE UPDATE ON medicines
    FOR EACH ROW
    EXECUTE FUNCTION fn_set_updated_at();

-- ─────────────────────────────────────────────────────────────────────────
-- 5. INDEXES (Section 4.3.4 of DB Design)
-- ─────────────────────────────────────────────────────────────────────────

-- users
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email
    ON users (email);

CREATE INDEX IF NOT EXISTS idx_users_role
    ON users (role);

CREATE INDEX IF NOT EXISTS idx_users_is_active
    ON users (is_active);

-- medicines
CREATE INDEX IF NOT EXISTS idx_medicines_user_id
    ON medicines (user_id);

CREATE INDEX IF NOT EXISTS idx_medicines_expiry_date
    ON medicines (expiry_date);

CREATE INDEX IF NOT EXISTS idx_medicines_is_active
    ON medicines (is_active);

CREATE INDEX IF NOT EXISTS idx_medicines_user_active
    ON medicines (user_id, is_active);

CREATE INDEX IF NOT EXISTS idx_medicines_category
    ON medicines (category)
    WHERE category IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_medicines_barcode
    ON medicines (barcode)
    WHERE barcode IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_medicines_expiry_user
    ON medicines (user_id, expiry_date)
    WHERE is_active = TRUE;
