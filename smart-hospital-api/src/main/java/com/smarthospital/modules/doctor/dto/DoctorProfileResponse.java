package com.smarthospital.modules.doctor.dto;

import com.smarthospital.modules.doctor.domain.DoctorProfile;
import com.smarthospital.modules.hr.domain.Employee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record DoctorProfileResponse(
    UUID id,
    UUID employeeId,
    String employeeCode,
    String firstName,
    String lastName,
    String gender,
    String mobile,
    String email,
    String departmentId,
    String designationId,
    String status,
    LocalDate joinDate,
    String profilePhoto,
    String biography,
    String qualifications,
    Integer experienceYears,
    BigDecimal consultationFee,
    BigDecimal followUpFee,
    BigDecimal teleConsultationFee,
    String languages,
    String awards,
    String achievements,
    String publications,
    boolean onlineBookingEnabled,
    boolean displayOnPortal,
    Set<SpecializationResponse> specializations
) {
    public static DoctorProfileResponse from(DoctorProfile p, Employee e) {
        return new DoctorProfileResponse(
            p.getId(),
            e.getId(),
            e.getEmployeeCode(),
            e.getFirstName(),
            e.getLastName(),
            e.getGender() != null ? e.getGender().name() : null,
            e.getMobile(),
            e.getEmail(),
            e.getDepartmentId() != null ? e.getDepartmentId().toString() : null,
            e.getDesignationId() != null ? e.getDesignationId().toString() : null,
            e.getStatus().name(),
            e.getJoinDate(),
            p.getProfilePhoto(),
            p.getBiography(),
            p.getQualifications(),
            p.getExperienceYears(),
            p.getConsultationFee(),
            p.getFollowUpFee(),
            p.getTeleConsultationFee(),
            p.getLanguages(),
            p.getAwards(),
            p.getAchievements(),
            p.getPublications(),
            p.isOnlineBookingEnabled(),
            p.isDisplayOnPortal(),
            p.getSpecializations().stream().map(SpecializationResponse::from).collect(Collectors.toSet())
        );
    }
}
