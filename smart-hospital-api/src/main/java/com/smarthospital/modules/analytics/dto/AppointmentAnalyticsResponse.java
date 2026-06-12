package com.smarthospital.modules.analytics.dto;

import java.util.List;

public record AppointmentAnalyticsResponse(
    long totalAppointments,
    long completed,
    long cancelled,
    long noShow,
    long rescheduled,

    List<TrendPoint> dailyTrend,
    List<NameValuePoint> statusDistribution,
    List<NameValuePoint> byDoctor,
    List<NameValuePoint> byDepartment,
    List<HeatmapCell> peakHoursHeatmap
) {
    public record HeatmapCell(int hour, String weekday, long count) {}
}
