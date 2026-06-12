package com.smarthospital.modules.finance.repository;

import com.smarthospital.modules.finance.domain.ExpenseEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseEntryRepository extends JpaRepository<ExpenseEntry, UUID> {

    Page<ExpenseEntry> findByEntryDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    Page<ExpenseEntry> findByEntryDateBetweenAndCategoryId(
            LocalDate from, LocalDate to, UUID categoryId, Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM expense_entries WHERE entry_date = :date",
           nativeQuery = true)
    BigDecimal sumByDate(@Param("date") LocalDate date);

    @Query(value = "SELECT entry_date, COALESCE(SUM(amount), 0) FROM expense_entries " +
                   "WHERE entry_date BETWEEN :from AND :to " +
                   "GROUP BY entry_date ORDER BY entry_date",
           nativeQuery = true)
    List<Object[]> sumGroupedByDate(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM expense_entries WHERE entry_date BETWEEN :from AND :to",
           nativeQuery = true)
    BigDecimal sumBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT category_name, COALESCE(SUM(amount), 0) FROM expense_entries " +
                   "WHERE entry_date BETWEEN :from AND :to " +
                   "GROUP BY category_name ORDER BY SUM(amount) DESC",
           nativeQuery = true)
    List<Object[]> sumByCategoryName(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT COUNT(*) + 1 FROM expense_entries WHERE EXTRACT(YEAR FROM created_at) = :year",
           nativeQuery = true)
    long nextSequenceForYear(@Param("year") int year);
}
