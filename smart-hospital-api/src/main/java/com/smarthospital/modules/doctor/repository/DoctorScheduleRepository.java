package com.smarthospital.modules.doctor.repository;

import com.smarthospital.modules.doctor.domain.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, UUID> {
    List<DoctorSchedule> findByDoctorIdAndActiveTrue(UUID doctorId);

    @Modifying
    @Query("DELETE FROM DoctorSchedule s WHERE s.doctorId = :doctorId")
    void deleteByDoctorId(@Param("doctorId") UUID doctorId);
}
