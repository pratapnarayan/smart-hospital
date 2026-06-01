package com.smarthospital.modules.opd.repository;

import com.smarthospital.modules.opd.domain.OpdVisit;
import com.smarthospital.modules.opd.domain.OpdVisit.VisitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpdVisitRepository extends JpaRepository<OpdVisit, UUID> {

    Page<OpdVisit> findByPatientId(UUID patientId, Pageable pageable);

    Page<OpdVisit> findByVisitDate(LocalDate date, Pageable pageable);

    List<OpdVisit> findByPatientIdAndVisitStatus(UUID patientId, VisitStatus status);

    /** Daily summary count by status — used for dashboard widget */
    @Query("SELECT v.visitStatus, COUNT(v) FROM OpdVisit v WHERE v.visitDate = :date GROUP BY v.visitStatus")
    List<Object[]> countByStatusForDate(@Param("date") LocalDate date);

    /** Next visit number sequence — e.g. OPD-2026-00042
     *  Counts by visit_number prefix so it stays correct even when visit_date is backdated. */
    @Query(value = "SELECT COUNT(*) + 1 FROM opd_visits WHERE visit_number LIKE CONCAT('OPD-', :year, '-%')",
           nativeQuery = true)
    long nextSequenceForYear(@Param("year") int year);

    Optional<OpdVisit> findByVisitNumber(String visitNumber);
}
