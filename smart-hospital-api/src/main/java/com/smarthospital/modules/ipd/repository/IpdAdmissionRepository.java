package com.smarthospital.modules.ipd.repository;

import com.smarthospital.modules.ipd.domain.IpdAdmission;
import com.smarthospital.modules.ipd.domain.IpdAdmission.AdmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IpdAdmissionRepository extends JpaRepository<IpdAdmission, UUID> {

    Page<IpdAdmission> findByPatientId(UUID patientId, Pageable pageable);

    Page<IpdAdmission> findByStatus(AdmissionStatus status, Pageable pageable);

    Optional<IpdAdmission> findByAdmissionNumber(String admissionNumber);

    /** Checks if a bed is currently occupied by an active admission */
    boolean existsByBedIdAndStatus(UUID bedId, AdmissionStatus status);

    @Query(value = "SELECT COUNT(*) + 1 FROM ipd_admissions WHERE admission_number LIKE CONCAT('IPD-', :year, '-%')",
           nativeQuery = true)
    long nextSequenceForYear(@Param("year") int year);

    @Query("SELECT a.status, COUNT(a) FROM IpdAdmission a GROUP BY a.status")
    List<Object[]> countByStatus();
}
