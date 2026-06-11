package com.smarthospital.modules.finance.repository;

import com.smarthospital.modules.finance.domain.IncomeEntry;
import com.smarthospital.modules.finance.domain.IncomeEntry.SourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IncomeEntryRepository extends JpaRepository<IncomeEntry, UUID> {

    Page<IncomeEntry> findByEntryDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    Page<IncomeEntry> findByEntryDateBetweenAndSourceType(
            LocalDate from, LocalDate to, SourceType sourceType, Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM income_entries WHERE entry_date = :date",
           nativeQuery = true)
    BigDecimal sumByDate(@Param("date") LocalDate date);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM income_entries WHERE entry_date BETWEEN :from AND :to",
           nativeQuery = true)
    BigDecimal sumBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT source_type, COALESCE(SUM(amount), 0) FROM income_entries " +
                   "WHERE entry_date BETWEEN :from AND :to " +
                   "GROUP BY source_type ORDER BY SUM(amount) DESC",
           nativeQuery = true)
    List<Object[]> sumBySourceType(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT COALESCE(doctor_name, 'Unknown'), COALESCE(SUM(amount), 0) " +
                   "FROM income_entries WHERE entry_date BETWEEN :from AND :to " +
                   "AND doctor_name IS NOT NULL " +
                   "GROUP BY doctor_name ORDER BY SUM(amount) DESC LIMIT 10",
           nativeQuery = true)
    List<Object[]> sumByDoctorName(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT TO_CHAR(entry_date, 'Mon YYYY'), COALESCE(SUM(amount), 0) " +
                   "FROM income_entries WHERE entry_date BETWEEN :from AND :to " +
                   "GROUP BY TO_CHAR(entry_date, 'Mon YYYY'), DATE_TRUNC('month', entry_date) " +
                   "ORDER BY DATE_TRUNC('month', entry_date)",
           nativeQuery = true)
    List<Object[]> sumByMonth(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT COUNT(*) + 1 FROM income_entries WHERE EXTRACT(YEAR FROM created_at) = :year",
           nativeQuery = true)
    long nextSequenceForYear(@Param("year") int year);
}
