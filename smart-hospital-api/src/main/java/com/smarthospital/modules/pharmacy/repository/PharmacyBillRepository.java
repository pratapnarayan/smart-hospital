package com.smarthospital.modules.pharmacy.repository;

import com.smarthospital.modules.pharmacy.domain.PharmacyBill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PharmacyBillRepository extends JpaRepository<PharmacyBill, UUID> {

    Optional<PharmacyBill> findByBillNumber(String billNumber);

    Page<PharmacyBill> findByPatientId(UUID patientId, Pageable pageable);

    Page<PharmacyBill> findByCreatedAtBetween(Instant from, Instant to, Pageable pageable);

    /** Bill number sequence: PH-YYYYMMDD-NNN */
    @Query(value = "SELECT COUNT(*) + 1 FROM pharmacy_bills " +
                   "WHERE DATE(created_at) = CURRENT_DATE",
           nativeQuery = true)
    long nextDailySequence();

    // ── Analytics queries ────────────────────────────────────────────────────

    /** Sum of net_amount for bills created in [from, to) */
    @Query("SELECT COALESCE(SUM(b.netAmount), 0) FROM PharmacyBill b " +
           "WHERE b.createdAt >= :from AND b.createdAt < :to")
    java.math.BigDecimal sumNetAmountBetween(@Param("from") Instant from,
                                             @Param("to")   Instant to);

    /** Count of bills created in [from, to) */
    @Query("SELECT COUNT(b) FROM PharmacyBill b " +
           "WHERE b.createdAt >= :from AND b.createdAt < :to")
    long countBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** Sum of net_amount for bills created today */
    @Query(value = "SELECT COALESCE(SUM(net_amount), 0) FROM pharmacy_bills " +
                   "WHERE (created_at AT TIME ZONE 'UTC')::date = (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date",
           nativeQuery = true)
    java.math.BigDecimal sumNetAmountToday();

    /**
     * Top N medicines by revenue (sum of total_price on bill items) for bills in range.
     * Returns Object[] { medicineName(String), revenue(BigDecimal) }
     */
    @Query(value = "SELECT i.medicine_name, SUM(i.total_price) AS revenue " +
                   "FROM pharmacy_bill_items i " +
                   "JOIN pharmacy_bills b ON b.id = i.bill_id " +
                   "WHERE b.created_at >= :from AND b.created_at < :to " +
                   "GROUP BY i.medicine_name " +
                   "ORDER BY revenue DESC " +
                   "LIMIT 10",
           nativeQuery = true)
    List<Object[]> topMedicinesByRevenue(@Param("from") Instant from,
                                         @Param("to")   Instant to);

    /**
     * Revenue grouped by medicine category name for bills in range.
     * Returns Object[] { categoryName(String), revenue(BigDecimal) }
     */
    @Query(value = "SELECT mc.name, SUM(i.total_price) AS revenue " +
                   "FROM pharmacy_bill_items i " +
                   "JOIN pharmacy_bills b ON b.id = i.bill_id " +
                   "JOIN medicine_batches mb ON mb.id = i.batch_id " +
                   "JOIN medicines m ON m.id = mb.medicine_id " +
                   "JOIN medicine_categories mc ON mc.id = m.category_id " +
                   "WHERE b.created_at >= :from AND b.created_at < :to " +
                   "GROUP BY mc.name " +
                   "ORDER BY revenue DESC",
           nativeQuery = true)
    List<Object[]> revenueByCategory(@Param("from") Instant from,
                                     @Param("to")   Instant to);

    /**
     * Daily revenue trend for bills in range.
     * Returns Object[] { day(String yyyy-MM-dd), revenue(BigDecimal) }
     */
    @Query(value = "SELECT TO_CHAR(DATE(created_at AT TIME ZONE 'UTC'), 'YYYY-MM-DD') AS day, " +
                   "       SUM(net_amount) AS revenue " +
                   "FROM pharmacy_bills " +
                   "WHERE created_at >= :from AND created_at < :to " +
                   "GROUP BY day " +
                   "ORDER BY day",
           nativeQuery = true)
    List<Object[]> dailyRevenueTrend(@Param("from") Instant from,
                                     @Param("to")   Instant to);
}
