package com.smarthospital.modules.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinanceAnalyticsResponse(
    BigDecimal totalRevenue,
    BigDecimal totalExpenses,
    BigDecimal netProfit,
    double collectionEfficiencyPct,

    List<TrendPoint> dailyRevenue,
    List<NameValuePoint> revenueBySource,
    List<NameValuePoint> revenueByDoctor,
    List<NameValuePoint> monthlyComparison,
    List<TrendPoint> expenseTrend
) {}
