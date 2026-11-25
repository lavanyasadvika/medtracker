package com.medtracker.entity;

/**
 * Application roles mapped to the {@code user_role} PostgreSQL ENUM.
 * <p>
 * V1 DDL defined ADMIN and USER.
 * V3 migration adds PHARMACIST and VIEWER to align with this enum.
 */
public enum Role {
    /** Full system access – can manage users and all medicines. */
    ADMIN,
    /** Standard authenticated user – manages their own medicines. */
    USER,
    /** Pharmacy staff – can dispense and restock medicines. */
    PHARMACIST,
    /** Read-only access – can view but not modify medicines. */
    VIEWER
}
