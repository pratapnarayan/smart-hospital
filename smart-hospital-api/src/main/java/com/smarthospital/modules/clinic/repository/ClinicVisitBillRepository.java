package com.smarthospital.modules.clinic.repository;

import com.smarthospital.modules.clinic.domain.ClinicVisitBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicVisitBillRepository extends JpaRepository<ClinicVisitBill, UUID> {

    List<ClinicVisitBill> findByOpdVisitId(UUID opdVisitId);

    List<ClinicVisitBill> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    @Query(value = """
            SELECT COALESCE(MAX(CAST(SPLIT_PART(bill_number, '-', 3) AS INTEGER)), 0) + 1
            FROM clinic_visit_bills
            WHERE visit_date = :date
            """, nativeQuery = true)
    int nextDailySequence(@Param("date") LocalDate date);

    @Query("SELECT SUM(b.totalAmount) FROM ClinicVisitBill b WHERE b.visitDate = :date AND b.status = 'FINALIZED'")
    Optional<BigDecimal> sumFinalizedAmountByDate(@Param("date") LocalDate date);
}
