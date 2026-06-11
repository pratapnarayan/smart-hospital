package com.smarthospital.modules.doctor.repository;

import com.smarthospital.modules.doctor.domain.DoctorProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {
    Optional<DoctorProfile> findByEmployeeId(UUID employeeId);
    boolean existsByEmployeeId(UUID employeeId);

    /** Count doctors whose linked employee is ACTIVE. */
    @Query(value = "SELECT COUNT(DISTINCT dp.id) FROM doctor_profiles dp " +
                   "JOIN employees e ON e.id = dp.employee_id AND e.deleted_at IS NULL AND e.status = 'ACTIVE'",
           nativeQuery = true)
    long countActiveDoctors();

    /**
     * Count active doctors who have a schedule entry for today's day of week.
     */
    @Query(value = "SELECT COUNT(DISTINCT dp.id) FROM doctor_profiles dp " +
                   "JOIN employees e ON e.id = dp.employee_id AND e.deleted_at IS NULL AND e.status = 'ACTIVE' " +
                   "JOIN doctor_schedules ds ON ds.doctor_id = dp.id AND ds.active = true " +
                   "WHERE ds.day_of_week = UPPER(TO_CHAR(CURRENT_DATE, 'Day'))::text",
           nativeQuery = true)
    long countAvailableTodayDoctors();

    /**
     * Appointment counts per doctor for the given date range (COMPLETED appointments only).
     * Returns rows: [doctorName, specialization, apptCount]
     */
    @Query(value = """
        SELECT a.doctor_name,
               COALESCE((SELECT s.name FROM specializations s
                         JOIN doctor_specializations ds ON ds.specialization_id = s.id
                         WHERE ds.doctor_profile_id = dp.id LIMIT 1), '') AS specialization,
               COUNT(a.id) AS appt_count
        FROM appointments a
        LEFT JOIN employees e ON LOWER(e.first_name || ' ' || e.last_name) = LOWER(a.doctor_name)
                              AND e.deleted_at IS NULL
        LEFT JOIN doctor_profiles dp ON dp.employee_id = e.id
        WHERE a.appointment_date BETWEEN :from AND :to
          AND a.status = 'COMPLETED'
          AND a.doctor_name IS NOT NULL
        GROUP BY a.doctor_name, dp.id
        ORDER BY COUNT(a.id) DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> countAppointmentsByDoctor(@Param("from") LocalDate from, @Param("to") LocalDate to);

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
