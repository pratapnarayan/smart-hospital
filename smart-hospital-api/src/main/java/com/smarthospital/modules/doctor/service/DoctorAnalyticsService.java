package com.smarthospital.modules.doctor.service;

import com.smarthospital.modules.analytics.dto.DoctorAnalyticsResponse;
import com.smarthospital.modules.analytics.dto.NameValuePoint;
import com.smarthospital.modules.doctor.repository.DoctorProfileRepository;
import com.smarthospital.modules.finance.repository.IncomeEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DoctorAnalyticsService {

    private final DoctorProfileRepository doctorRepo;
    private final IncomeEntryRepository incomeRepo;

    public DoctorAnalyticsService(DoctorProfileRepository doctorRepo,
                                  IncomeEntryRepository incomeRepo) {
        this.doctorRepo = doctorRepo;
        this.incomeRepo = incomeRepo;
    }

    public DoctorAnalyticsResponse getAnalytics(LocalDate from, LocalDate to) {
        long totalDoctors  = doctorRepo.count();
        long activeDoctors = doctorRepo.countActiveDoctors();
        double daysInRange = Math.max(1, ChronoUnit.DAYS.between(from, to) + 1);

        // Build revenue map by doctor name
        Map<String, BigDecimal> revenueMap = incomeRepo.sumByDoctorName(from, to).stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? row[0].toString() : "Unknown",
                        row -> row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO
                ));

        // Get appointment counts by doctor
        List<Object[]> apptRows = doctorRepo.countAppointmentsByDoctor(from, to);

        // Build leaderboard
        List<DoctorAnalyticsResponse.DoctorStatEntry> leaderboard = apptRows.stream()
                .limit(10)
                .map(row -> {
                    String name  = row[0] != null ? row[0].toString() : "Unknown";
                    String spec  = row[1] != null ? row[1].toString() : "";
                    long appts   = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                    BigDecimal rev = revenueMap.getOrDefault(name, BigDecimal.ZERO);
                    double util  = appts > 0 ? Math.min(100.0, appts / daysInRange * 100) : 0.0;
                    return new DoctorAnalyticsResponse.DoctorStatEntry(name, spec, appts, rev, util);
                })
                .toList();

        List<NameValuePoint> revenueByDoctor = revenueMap.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .map(e -> new NameValuePoint(e.getKey(), e.getValue().doubleValue()))
                .toList();

        List<NameValuePoint> apptsByDoctor = apptRows.stream()
                .limit(10)
                .map(row -> new NameValuePoint(
                        row[0] != null ? row[0].toString() : "Unknown",
                        row[2] != null ? ((Number) row[2]).doubleValue() : 0.0))
                .toList();

        return new DoctorAnalyticsResponse(totalDoctors, activeDoctors,
                leaderboard, revenueByDoctor, apptsByDoctor);
    }

    public long getDoctorsAvailableToday() {
        return doctorRepo.countAvailableTodayDoctors();
    }
}
