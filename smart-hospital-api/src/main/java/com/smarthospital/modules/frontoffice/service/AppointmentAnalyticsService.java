package com.smarthospital.modules.frontoffice.service;

import com.smarthospital.modules.analytics.dto.AppointmentAnalyticsResponse;
import com.smarthospital.modules.analytics.dto.NameValuePoint;
import com.smarthospital.modules.analytics.dto.TrendPoint;
import com.smarthospital.modules.frontoffice.domain.Appointment.AppointmentStatus;
import com.smarthospital.modules.frontoffice.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AppointmentAnalyticsService {

    private final AppointmentRepository appointmentRepo;

    public AppointmentAnalyticsService(AppointmentRepository appointmentRepo) {
        this.appointmentRepo = appointmentRepo;
    }

    public AppointmentAnalyticsResponse getAnalytics(LocalDate from, LocalDate to) {
        long total       = appointmentRepo.countByDateRange(from, to);
        long completed   = appointmentRepo.countByStatusAndDateRange(AppointmentStatus.COMPLETED.name(), from, to);
        long cancelled   = appointmentRepo.countByStatusAndDateRange(AppointmentStatus.CANCELLED.name(), from, to);
        long noShow      = appointmentRepo.countByStatusAndDateRange(AppointmentStatus.NO_SHOW.name(), from, to);
        long scheduled = appointmentRepo.countByStatusAndDateRange(AppointmentStatus.SCHEDULED.name(), from, to);

        List<TrendPoint> dailyTrend = buildDailyTrend(from, to);

        List<NameValuePoint> statusDist = List.of(
                new NameValuePoint("Completed", completed),
                new NameValuePoint("Cancelled", cancelled),
                new NameValuePoint("No Show",   noShow),
                new NameValuePoint("Scheduled", scheduled)
        );

        List<NameValuePoint> byDoctor = appointmentRepo.countByDoctor(from, to).stream()
                .map(r -> new NameValuePoint(
                        r[0] != null ? r[0].toString() : "Unknown",
                        ((Number) r[1]).doubleValue()))
                .limit(10)
                .toList();

        List<NameValuePoint> byDept = appointmentRepo.countByDepartment(from, to).stream()
                .map(r -> new NameValuePoint(
                        r[0] != null ? r[0].toString() : "Unknown",
                        ((Number) r[1]).doubleValue()))
                .limit(10)
                .toList();

        List<AppointmentAnalyticsResponse.HeatmapCell> heatmap =
                appointmentRepo.countByHourAndWeekday(from, to).stream()
                        .map(r -> new AppointmentAnalyticsResponse.HeatmapCell(
                                ((Number) r[0]).intValue(),
                                r[1].toString(),
                                ((Number) r[2]).longValue()))
                        .toList();

        return new AppointmentAnalyticsResponse(
                total, completed, cancelled, noShow, scheduled,
                dailyTrend, statusDist, byDoctor, byDept, heatmap);
    }

    public long getTodayAppointments() {
        LocalDate today = LocalDate.now();
        return appointmentRepo.countByDateRange(today, today);
    }

    private List<TrendPoint> buildDailyTrend(LocalDate from, LocalDate to) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        List<TrendPoint> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            long count = appointmentRepo.countByDateRange(d, d);
            result.add(new TrendPoint(d.format(fmt), count));
        }
        return result;
    }
}
