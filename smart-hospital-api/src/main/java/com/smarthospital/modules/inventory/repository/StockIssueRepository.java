package com.smarthospital.modules.inventory.repository;

import com.smarthospital.modules.inventory.domain.StockIssue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StockIssueRepository extends JpaRepository<StockIssue, UUID> {

    Page<StockIssue> findByIssueDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    Page<StockIssue> findByItemIdAndIssueDateBetween(
            UUID itemId, LocalDate from, LocalDate to, Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM stock_issues WHERE issue_date = CURRENT_DATE",
           nativeQuery = true)
    long countToday();

    @Query(value = "SELECT COUNT(*) + 1 FROM stock_issues WHERE EXTRACT(YEAR FROM created_at) = :year",
           nativeQuery = true)
    long nextSequenceForYear(@Param("year") int year);

    /** Top N items by total issued quantity in period: [item_name, SUM(quantity)] */
    @Query(value = """
        SELECT item_name, CAST(SUM(quantity) AS bigint)
        FROM stock_issues
        WHERE issue_date BETWEEN :from AND :to
        GROUP BY item_id, item_name
        ORDER BY SUM(quantity) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> topIssuedItems(@Param("from") LocalDate from,
                                  @Param("to") LocalDate to,
                                  @Param("limit") int limit);

    /** Items with non-zero stock and lowest issue totals in period: [item_name, SUM(quantity)] */
    @Query(value = """
        SELECT ii.name, COALESCE(CAST(SUM(si.quantity) AS bigint), 0) AS total_issued
        FROM inventory_items ii
        LEFT JOIN stock_issues si ON si.item_id = ii.id
            AND si.issue_date BETWEEN :from AND :to
        WHERE ii.current_stock > 0
        GROUP BY ii.id, ii.name
        ORDER BY total_issued ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> slowMovingItems(@Param("from") LocalDate from,
                                   @Param("to") LocalDate to,
                                   @Param("limit") int limit);
}
