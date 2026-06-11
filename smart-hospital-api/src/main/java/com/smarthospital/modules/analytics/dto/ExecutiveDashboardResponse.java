package com.smarthospital.modules.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExecutiveDashboardResponse(
    BigDecimal todayRevenue,
    BigDecimal monthRevenue,
    long totalPatients,
    long todayAppointments,
    BigDecimal pendingPayments,
    long doctorsAvailableToday,
    long currentAdmissions,
    long labTestsToday,
    BigDecimal medicineSalesToday,
    long inventoryAlerts,

    Double todayRevenueTrend,
    Double monthRevenueTrend,
    Double totalPatientsTrend,
    Double todayAppointmentsTrend,

    List<TrendPoint> revenueTrend,
    List<TrendPoint> patientGrowth,
    List<NameValuePoint> revenueBySource,
    List<NameValuePoint> topDoctors,
    List<NameValuePoint> departmentRevenue
) {}
