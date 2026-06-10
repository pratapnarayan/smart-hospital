package com.smarthospital.modules.doctor.repository;

import com.smarthospital.modules.doctor.domain.DoctorProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {
    Optional<DoctorProfile> findByEmployeeId(UUID employeeId);
    boolean existsByEmployeeId(UUID employeeId);

    /**
     * Search doctor profiles with optional filters.
     * Uses a native query to join with employees table across schema boundaries.
     * Filters by search name, department, and specialization.
     */
    @Query(value = """
        SELECT DISTINCT dp.* FROM doctor_profiles dp
        LEFT JOIN doctor_specializations ds ON ds.doctor_profile_id = dp.id
        JOIN employees e ON e.id = dp.employee_id AND e.deleted_at IS NULL AND e.status = 'ACTIVE'
        WHERE (:search IS NULL OR LOWER(e.first_name || ' ' || e.last_name) LIKE LOWER('%' || :search || '%'))
          AND (CAST(:deptId AS uuid) IS NULL OR e.department_id = CAST(:deptId AS uuid))
          AND (CAST(:specId AS uuid) IS NULL OR ds.specialization_id = CAST(:specId AS uuid))
        """,
        countQuery = """
        SELECT COUNT(DISTINCT dp.id) FROM doctor_profiles dp
        LEFT JOIN doctor_specializations ds ON ds.doctor_profile_id = dp.id
        JOIN employees e ON e.id = dp.employee_id AND e.deleted_at IS NULL AND e.status = 'ACTIVE'
        WHERE (:search IS NULL OR LOWER(e.first_name || ' ' || e.last_name) LIKE LOWER('%' || :search || '%'))
          AND (CAST(:deptId AS uuid) IS NULL OR e.department_id = CAST(:deptId AS uuid))
          AND (CAST(:specId AS uuid) IS NULL OR ds.specialization_id = CAST(:specId AS uuid))
        """,
        nativeQuery = true)
    Page<DoctorProfile> search(
        @Param("search") String search,
        @Param("deptId") UUID deptId,
        @Param("specId") UUID specId,
        Pageable pageable);
}
