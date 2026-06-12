package com.smarthospital.modules.pathology.repository;

import com.smarthospital.modules.pathology.domain.LabOrder;
import com.smarthospital.modules.pathology.domain.LabOrder.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LabOrderRepository extends JpaRepository<LabOrder, UUID> {

    Page<LabOrder> findByPatientId(UUID patientId, Pageable pageable);

    Page<LabOrder> findByStatus(OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);

    @Query(value = "SELECT COUNT(*) + 1 FROM lab_orders WHERE EXTRACT(YEAR FROM created_at) = :year",
           nativeQuery = true)
    long nextSequenceForYear(@Param("year") int year);

    // ── Analytics queries ────────────────────────────────────────────────────

    /** Count of orders (excluding CANCELLED) created in [from, to) */
    @Query("SELECT COUNT(o) FROM LabOrder o " +
           "WHERE o.createdAt >= :from AND o.createdAt < :to " +
           "AND o.status <> com.smarthospital.modules.pathology.domain.LabOrder.OrderStatus.CANCELLED")
    long countTestsPerformed(@Param("from") Instant from, @Param("to") Instant to);

    /** Sum of net_amount for orders in [from, to) excluding CANCELLED */
    @Query("SELECT COALESCE(SUM(o.netAmount), 0) FROM LabOrder o " +
           "WHERE o.createdAt >= :from AND o.createdAt < :to " +
           "AND o.status <> com.smarthospital.modules.pathology.domain.LabOrder.OrderStatus.CANCELLED")
    java.math.BigDecimal sumRevenueInRange(@Param("from") Instant from,
                                           @Param("to")   Instant to);

    /** Count of orders with status PENDING or IN_PROGRESS (pending reports) */
    @Query("SELECT COUNT(o) FROM LabOrder o " +
           "WHERE o.status = com.smarthospital.modules.pathology.domain.LabOrder.OrderStatus.PENDING " +
           "   OR o.status = com.smarthospital.modules.pathology.domain.LabOrder.OrderStatus.IN_PROGRESS")
    long countPendingReports();

    /**
     * Daily test count trend in range.
     * Returns Object[] { day(String), count(Long) }
     */
    @Query(value = "SELECT TO_CHAR(DATE(created_at AT TIME ZONE 'UTC'), 'YYYY-MM-DD') AS day, " +
                   "       COUNT(*) " +
                   "FROM lab_orders " +
                   "WHERE created_at >= :from AND created_at < :to " +
                   "  AND status <> 'CANCELLED' " +
                   "GROUP BY day " +
                   "ORDER BY day",
           nativeQuery = true)
    List<Object[]> dailyTestsTrend(@Param("from") Instant from,
                                   @Param("to")   Instant to);

    /**
     * Daily revenue trend in range.
     * Returns Object[] { day(String), revenue(BigDecimal) }
     */
    @Query(value = "SELECT TO_CHAR(DATE(created_at AT TIME ZONE 'UTC'), 'YYYY-MM-DD') AS day, " +
                   "       SUM(net_amount) " +
                   "FROM lab_orders " +
                   "WHERE created_at >= :from AND created_at < :to " +
                   "  AND status <> 'CANCELLED' " +
                   "GROUP BY day " +
                   "ORDER BY day",
           nativeQuery = true)
    List<Object[]> dailyRevenueTrend(@Param("from") Instant from,
                                     @Param("to")   Instant to);

    /**
     * Top 10 tests by occurrence in range.
     * Returns Object[] { testName(String), count(Long) }
     */
    @Query(value = "SELECT i.test_name, COUNT(*) AS cnt " +
                   "FROM lab_order_items i " +
                   "JOIN lab_orders o ON o.id = i.order_id " +
                   "WHERE o.created_at >= :from AND o.created_at < :to " +
                   "  AND o.status <> 'CANCELLED' " +
                   "GROUP BY i.test_name " +
                   "ORDER BY cnt DESC " +
                   "LIMIT 10",
           nativeQuery = true)
    List<Object[]> topTests(@Param("from") Instant from,
                            @Param("to")   Instant to);

    /**
     * Order status distribution in range.
     * Returns Object[] { status(String), count(Long) }
     */
    @Query(value = "SELECT status, COUNT(*) FROM lab_orders " +
                   "WHERE created_at >= :from AND created_at < :to " +
                   "GROUP BY status",
           nativeQuery = true)
    List<Object[]> statusDistribution(@Param("from") Instant from,
                                      @Param("to")   Instant to);

    /**
     * Orders by referral source (OPD / IPD / WALK_IN) in range.
     * Returns Object[] { sourceType(String), count(Long) }
     */
    @Query(value = "SELECT source_type, COUNT(*) FROM lab_orders " +
                   "WHERE created_at >= :from AND created_at < :to " +
                   "  AND status <> 'CANCELLED' " +
                   "GROUP BY source_type",
           nativeQuery = true)
    List<Object[]> byDepartmentReferral(@Param("from") Instant from,
                                        @Param("to")   Instant to);

    /** Count of orders created today (for Executive Dashboard) */
    @Query(value = "SELECT COUNT(*) FROM lab_orders " +
                   "WHERE (created_at AT TIME ZONE 'UTC')::date = (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date " +
                   "  AND status <> 'CANCELLED'",
           nativeQuery = true)
    long countTestsToday();
}
