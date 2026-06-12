package com.smarthospital.modules.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record DoctorAnalyticsResponse(
    long totalDoctors,
    long activeDoctors,

    List<DoctorStatEntry> leaderboard,
    List<NameValuePoint> revenueByDoctor,
    List<NameValuePoint> appointmentsByDoctor
) {
    public record DoctorStatEntry(
        String doctorName,
        String specialization,
        long appointmentsCompleted,
        BigDecimal revenueGenerated,
        double utilizationPct
    ) {}
}
