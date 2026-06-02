package com.smarthospital.modules.bloodbank.repository;

import com.smarthospital.modules.bloodbank.domain.BloodGroup;
import com.smarthospital.modules.bloodbank.domain.BloodUnit;
import com.smarthospital.modules.bloodbank.domain.BloodUnit.UnitStatus;
import com.smarthospital.modules.bloodbank.domain.ComponentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BloodUnitRepository extends JpaRepository<BloodUnit, UUID> {

    Page<BloodUnit> findByStatus(UnitStatus status, Pageable pageable);

    Page<BloodUnit> findByBloodGroupAndStatus(BloodGroup bloodGroup, UnitStatus status, Pageable pageable);

    Page<BloodUnit> findByBloodGroupAndComponentTypeAndStatus(
            BloodGroup bloodGroup, ComponentType componentType, UnitStatus status, Pageable pageable);

    /** Available units for issue, oldest expiry first (FEFO) */
    List<BloodUnit> findByBloodGroupAndComponentTypeAndStatusOrderByExpiryDateAsc(
            BloodGroup bloodGroup, ComponentType componentType, UnitStatus status);

    List<BloodUnit> findByBloodGroupAndStatusOrderByExpiryDateAsc(BloodGroup bloodGroup, UnitStatus status);

    List<BloodUnit> findByStatusOrderByExpiryDateAsc(UnitStatus status);

    @Query("SELECT u.bloodGroup, COUNT(u) FROM BloodUnit u WHERE u.status = :status GROUP BY u.bloodGroup")
    List<Object[]> countByBloodGroupAndStatus(@Param("status") UnitStatus status);

    @Query("SELECT COUNT(u) FROM BloodUnit u WHERE u.status = :status")
    long countByStatus(@Param("status") UnitStatus status);

    @Query(value = "SELECT COUNT(*) + 1 FROM blood_units WHERE EXTRACT(YEAR FROM created_at) = :year",
           nativeQuery = true)
    long nextSequenceForYear(@Param("year") int year);
}
