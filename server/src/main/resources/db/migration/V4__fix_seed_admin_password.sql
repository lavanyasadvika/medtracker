-- ═══════════════════════════════════════════════════════════════════════════
-- MedTracker – V4 Fix Seed Admin Password
--
-- The BCrypt hash inserted by V2__seed_admin.sql does not actually verify
-- against "Admin@123" (its own documented password) — logging in as the
-- bootstrap admin with those credentials fails on any fresh database.
-- V2 is already applied (and checksum-validated) on existing databases, so
-- it cannot be edited in place; this migration repairs the stored hash.
--
-- New BCrypt hash generated with strength 12 for "Admin@123":
--   $2a$12$BV40IJSfRDZpM8UkYHH4A.IomZ2hRj8hx6kFnHkRdlqPbYfq9.6WG
-- ═══════════════════════════════════════════════════════════════════════════

UPDATE users
SET password_hash = '$2a$12$BV40IJSfRDZpM8UkYHH4A.IomZ2hRj8hx6kFnHkRdlqPbYfq9.6WG',
    updated_at = NOW()
WHERE email = 'admin@medtracker.local';
