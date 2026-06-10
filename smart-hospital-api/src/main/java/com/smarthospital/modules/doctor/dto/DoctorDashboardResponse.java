package com.smarthospital.modules.doctor.dto;

public record DoctorDashboardResponse(
    long totalDoctors,
    long activeDoctors,
    long availableToday,
    long totalSpecializations
) {}
