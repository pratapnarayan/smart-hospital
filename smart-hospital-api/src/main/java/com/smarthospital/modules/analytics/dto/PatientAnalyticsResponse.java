package com.smarthospital.modules.analytics.dto;

import java.util.List;

public record PatientAnalyticsResponse(
    long totalPatients,
    long newPatientsThisPeriod,
    long returningPatients,
    double retentionRatePct,

    List<TrendPoint> registrationTrend,
    List<NameValuePoint> genderDistribution,
    List<NameValuePoint> ageDistribution,
    List<NameValuePoint> bloodGroupDistribution,
    List<NameValuePoint> patientsByDepartment
) {}
