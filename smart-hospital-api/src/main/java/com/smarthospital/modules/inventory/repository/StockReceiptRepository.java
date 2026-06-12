package com.smarthospital.modules.inventory.repository;

import com.smarthospital.modules.inventory.domain.StockReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StockReceiptRepository extends JpaRepository<StockReceipt, UUID> {

    Page<StockReceipt> findByEntryDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    Page<StockReceipt> findByItemIdAndEntryDateBetween(
            UUID itemId, LocalDate from, LocalDate to, Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM stock_receipts WHERE entry_date = CURRENT_DATE",
           nativeQuery = true)
    long countToday();

    @Query(value = "SELECT COALESCE(SUM(total_cost), 0) FROM stock_receipts WHERE entry_date = CURRENT_DATE",
           nativeQuery = true)
    BigDecimal sumTodayCost();

    @Query(value = "SELECT COUNT(*) + 1 FROM stock_receipts WHERE EXTRACT(YEAR FROM created_at) = :year",
           nativeQuery = true)
    long nextSequenceForYear(@Param("year") int year);

    /** Daily receipt total_cost sums between dates: [entry_date, SUM(total_cost)] */
    @Query(value = """
        SELECT entry_date::text, COALESCE(SUM(total_cost), 0)
        FROM stock_receipts
        WHERE entry_date BETWEEN :from AND :to
        GROUP BY entry_date
        ORDER BY entry_date
        """, nativeQuery = true)
    List<Object[]> dailyReceiptTotals(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Total stock value approximation: last known unit_cost per item * current_stock */
    @Query(value = """
        SELECT COALESCE(SUM(ii.current_stock * latest.unit_cost), 0)
        FROM inventory_items ii
        JOIN (
            SELECT DISTINCT ON (item_id) item_id, unit_cost
            FROM stock_receipts
            WHERE unit_cost IS NOT NULL
            ORDER BY item_id, entry_date DESC, created_at DESC
        ) latest ON latest.item_id = ii.id
        """, nativeQuery = true)
    java.math.BigDecimal totalStockValue();
}
