package com.smarthospital.modules.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record PharmacyAnalyticsResponse(
    BigDecimal totalMedicineRevenue,
    long totalBillsIssued,
    long lowStockAlerts,
    long expiryAlerts,

    List<NameValuePoint> topMedicinesByRevenue,
    List<NameValuePoint> stockHealthDistribution,
    List<NameValuePoint> revenueByCategory,
    List<TrendPoint> revenueTrend
) {}
