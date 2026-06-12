package com.smarthospital.modules.pharmacy.repository;

import com.smarthospital.modules.pharmacy.domain.MedicineBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


public interface MedicineBatchRepository extends JpaRepository<MedicineBatch, UUID> {

    List<MedicineBatch> findByMedicineIdOrderByExpiryDateAsc(UUID medicineId);

    /** Non-expired batches with stock, FEFO order (First Expiry First Out) */
    @Query("SELECT b FROM MedicineBatch b WHERE b.medicine.id = :medicineId " +
           "AND b.quantity > 0 AND b.expiryDate > :today ORDER BY b.expiryDate ASC")
    List<MedicineBatch> findAvailableBatches(@Param("medicineId") UUID medicineId,
                                             @Param("today") LocalDate today);

    /** Batches expiring within the given number of days */
    @Query("SELECT b FROM MedicineBatch b WHERE b.expiryDate <= :cutoff AND b.quantity > 0")
    List<MedicineBatch> findExpiringBefore(@Param("cutoff") LocalDate cutoff);

    boolean existsByMedicineIdAndBatchNumber(UUID medicineId, String batchNumber);

    /** Total available stock across all non-expired batches for a medicine */
    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM MedicineBatch b " +
           "WHERE b.medicine.id = :medicineId AND b.expiryDate > :today")
    int totalAvailableStock(@Param("medicineId") UUID medicineId,
                            @Param("today") LocalDate today);

    // ── Analytics queries ────────────────────────────────────────────────────

    /**
     * Count of batches where quantity <= medicine.reorder_level (low stock).
     * Joins medicines table to get per-medicine reorder level.
     */
    @Query(value = "SELECT COUNT(*) FROM medicine_batches mb " +
                   "JOIN medicines m ON m.id = mb.medicine_id AND m.deleted_at IS NULL " +
                   "WHERE mb.quantity <= m.reorder_level AND mb.quantity > 0",
           nativeQuery = true)
    long countLowStockBatches();

    /**
     * Count of batches expiring within the next :days days with remaining stock.
     */
    @Query("SELECT COUNT(b) FROM MedicineBatch b " +
           "WHERE b.quantity > 0 AND b.expiryDate <= :cutoff AND b.expiryDate >= :today")
    long countExpiringBatches(@Param("today") LocalDate today,
                              @Param("cutoff") LocalDate cutoff);

    /**
     * Stock health counts relative to each medicine's reorder level.
     * Returns Object[] { label(String), count(Long) }
     */
    @Query(value = "SELECT " +
                   "  CASE WHEN mb.quantity > m.reorder_level THEN 'In Stock' " +
                   "       WHEN mb.quantity > 0               THEN 'Low' " +
                   "       ELSE 'Out of Stock' END AS label, " +
                   "  COUNT(*) " +
                   "FROM medicine_batches mb " +
                   "JOIN medicines m ON m.id = mb.medicine_id " +
                   "GROUP BY label",
           nativeQuery = true)
    List<Object[]> stockHealthDistribution();
}
