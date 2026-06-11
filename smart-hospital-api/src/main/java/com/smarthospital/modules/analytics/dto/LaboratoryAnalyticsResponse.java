package com.smarthospital.modules.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record LaboratoryAnalyticsResponse(
    long totalTestsPerformed,
    BigDecimal totalRevenue,
    long pendingReports,

    List<TrendPoint> dailyTestsTrend,
    List<NameValuePoint> topTests,
    List<TrendPoint> revenueTrend,
    List<NameValuePoint> statusDistribution,
    List<NameValuePoint> byDepartmentReferral
) {}
