package com.smarthospital.modules.doctor.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record DoctorProfileRequest(
    UUID employeeId,
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
    Boolean onlineBookingEnabled,
    Boolean displayOnPortal,
    Set<UUID> specializationIds
) {}
