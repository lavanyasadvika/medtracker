-- ═══════════════════════════════════════════════════════════════════════════
-- MedTracker – V2 Bootstrap Admin Seed
-- Inserts the initial ADMIN user used for first-time system access.
--
-- Credentials:
--   email    : admin@medtracker.local
--   password : Admin@123  (BCrypt hash below)
--
-- BCrypt hash generated with strength 12 for "Admin@123":
--   $2a$12$RIZBuMkDkMBpFNKUCnFsQOQ7B3OGVNpDJqVFfMkuNpOJhLqkXq3Gy
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO users (
    name,
    email,
    password_hash,
    role,
    is_active,
    created_by,
    created_at,
    updated_at
)
SELECT
    'System Administrator',
    'admin@medtracker.local',
    '$2a$12$RIZBuMkDkMBpFNKUCnFsQOQ7B3OGVNpDJqVFfMkuNpOJhLqkXq3Gy',
    'ADMIN',
    TRUE,
    NULL,       -- bootstrap admin has no creator
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@medtracker.local'
);
