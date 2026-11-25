package com.medtracker.repository;

import com.medtracker.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Medicine} entities.
 *
 * <p>CRUD endpoints are protected with controller-level RBAC. Repository
 * methods used by those endpoints always exclude soft-deleted records unless
 * a method explicitly states otherwise.
 */
@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long>, JpaSpecificationExecutor<Medicine> {

    /**
     * Returns a single active medicine by id.
     */
    Optional<Medicine> findByIdAndIsActiveTrue(Long id);

    /**
     * Returns all active medicines sorted by medicine name.
     */
    List<Medicine> findByIsActiveTrueOrderByDrugNameAsc();

    /**
     * Returns all active medicines owned by the given user.
     */
    List<Medicine> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * Finds a single medicine by id, but only if it belongs to the given user.
     * Use this instead of {@link #findById} to prevent IDOR vulnerabilities.
     */
    Optional<Medicine> findByIdAndUserId(Long id, Long userId);

    /**
     * Active medicines expiring within the given date range for scheduled alerts.
     */
    @Query("SELECT m FROM Medicine m "
            + "WHERE m.expiryDate BETWEEN :from AND :to "
            + "  AND m.isActive = true")
    List<Medicine> findExpiringSoon(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Active medicines already expired for scheduled alerts.
     */
    @Query("SELECT m FROM Medicine m "
            + "WHERE m.expiryDate < :today "
            + "  AND m.isActive = true")
    List<Medicine> findExpired(@Param("today") LocalDate today);

    /**
     * Active medicines at or below their configured threshold, falling back to the default threshold.
     */
    @Query("SELECT m FROM Medicine m "
            + "WHERE m.quantity <= COALESCE(m.lowStockThreshold, :defaultThreshold) "
            + "  AND m.isActive = true")
    List<Medicine> findLowStock(@Param("defaultThreshold") Integer defaultThreshold);

    /**
     * Medicines expiring within the given date range, scoped to one user.
     */
    @Query("SELECT m FROM Medicine m "
            + "WHERE m.userId = :userId "
            + "  AND m.expiryDate BETWEEN :from AND :to "
            + "  AND m.isActive = true")
    List<Medicine> findExpiringSoon(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Medicines already expired, scoped to one user.
     */
    @Query("SELECT m FROM Medicine m "
            + "WHERE m.userId = :userId "
            + "  AND m.expiryDate < :today "
            + "  AND m.isActive = true")
    List<Medicine> findExpired(
            @Param("userId") Long userId,
            @Param("today") LocalDate today);

    /**
     * Medicines at or below their low-stock threshold, scoped to one user.
     */
    @Query("SELECT m FROM Medicine m "
            + "WHERE m.userId = :userId "
            + "  AND m.lowStockThreshold IS NOT NULL "
            + "  AND m.quantity <= m.lowStockThreshold "
            + "  AND m.isActive = true")
    List<Medicine> findLowStock(@Param("userId") Long userId);
}
