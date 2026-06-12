package com.smarthospital.modules.frontoffice.repository;

import com.smarthospital.modules.frontoffice.domain.Appointment;
import com.smarthospital.modules.frontoffice.domain.Appointment.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Page<Appointment> findByAppointmentDate(LocalDate date, Pageable pageable);

    Page<Appointment> findByPatientId(UUID patientId, Pageable pageable);

    Page<Appointment> findByDoctorIdAndAppointmentDate(UUID doctorId, LocalDate date, Pageable pageable);

    List<Appointment> findByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status);

    long countByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status);

    /** All upcoming appointments (date >= today) for the given active statuses. */
    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate >= :fromDate AND a.status IN :statuses")
    Page<Appointment> findUpcoming(@Param("fromDate") LocalDate fromDate,
                                   @Param("statuses") Collection<AppointmentStatus> statuses,
                                   Pageable pageable);

    /** Upcoming appointments for a single patient, sorted earliest first. */
    @Query("SELECT a FROM Appointment a WHERE a.patientId = :patientId AND a.appointmentDate >= :fromDate AND a.status IN :statuses ORDER BY a.appointmentDate ASC, a.timeSlot ASC NULLS LAST")
    List<Appointment> findUpcomingByPatient(@Param("patientId") UUID patientId,
                                            @Param("fromDate") LocalDate fromDate,
                                            @Param("statuses") Collection<AppointmentStatus> statuses);

    @Query(value = "SELECT COUNT(*) + 1 FROM appointments WHERE appointment_number LIKE CONCAT('APT-', :year, '-%')",
           nativeQuery = true)
    long nextSequenceForYear(@Param("year") int year);

    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusNotIn(UUID doctorId, LocalDate appointmentDate, Collection<AppointmentStatus> excludedStatuses);

    default List<Appointment> findByDoctorIdAndDate(UUID doctorId, LocalDate date) {
        return findByDoctorIdAndAppointmentDateAndStatusNotIn(doctorId, date,
            List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW));
    }

    @Query(value = "SELECT COUNT(*) FROM appointments WHERE appointment_date BETWEEN :from AND :to",
           nativeQuery = true)
    long countByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT COUNT(*) FROM appointments WHERE status = :status AND appointment_date BETWEEN :from AND :to",
           nativeQuery = true)
    long countByStatusAndDateRange(@Param("status") String status,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to);

    @Query(value = "SELECT doctor_name, COUNT(*) FROM appointments " +
                   "WHERE appointment_date BETWEEN :from AND :to AND doctor_name IS NOT NULL " +
                   "GROUP BY doctor_name ORDER BY COUNT(*) DESC",
           nativeQuery = true)
    List<Object[]> countByDoctor(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT department, COUNT(*) FROM appointments " +
                   "WHERE appointment_date BETWEEN :from AND :to AND department IS NOT NULL " +
                   "GROUP BY department ORDER BY COUNT(*) DESC",
           nativeQuery = true)
    List<Object[]> countByDepartment(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = """
        SELECT EXTRACT(HOUR FROM CAST(time_slot AS TIME))::int AS hr,
               TO_CHAR(appointment_date, 'Dy') AS wd,
               COUNT(*) AS cnt
        FROM appointments
        WHERE appointment_date BETWEEN :from AND :to
          AND time_slot IS NOT NULL
        GROUP BY hr, wd
        ORDER BY hr, wd
        """, nativeQuery = true)
    List<Object[]> countByHourAndWeekday(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
