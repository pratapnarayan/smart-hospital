package com.smarthospital.modules.pathology.service;

import com.smarthospital.modules.analytics.dto.LaboratoryAnalyticsResponse;
import com.smarthospital.modules.analytics.dto.NameValuePoint;
import com.smarthospital.modules.analytics.dto.TrendPoint;
import com.smarthospital.modules.pathology.repository.LabOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PathologyAnalyticsService {

    private final LabOrderRepository labOrderRepo;

    public PathologyAnalyticsService(LabOrderRepository labOrderRepo) {
        this.labOrderRepo = labOrderRepo;
    }

    public LaboratoryAnalyticsResponse getAnalytics(LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long totalTests       = labOrderRepo.countTestsPerformed(fromInstant, toInstant);
        BigDecimal totalRev   = labOrderRepo.sumRevenueInRange(fromInstant, toInstant);
        long pendingReports   = labOrderRepo.countPendingReports();

        List<TrendPoint> dailyTestsTrend = labOrderRepo.dailyTestsTrend(fromInstant, toInstant)
                .stream()
                .map(row -> new TrendPoint(
                        row[0] != null ? row[0].toString() : "",
                        row[1] != null ? ((Number) row[1]).doubleValue() : 0.0))
                .toList();

        List<NameValuePoint> topTests = labOrderRepo.topTests(fromInstant, toInstant)
                .stream()
                .map(row -> new NameValuePoint(
                        row[0] != null ? row[0].toString() : "Unknown",
                        row[1] != null ? ((Number) row[1]).doubleValue() : 0.0))
                .toList();

        List<TrendPoint> revenueTrend = labOrderRepo.dailyRevenueTrend(fromInstant, toInstant)
                .stream()
                .map(row -> new TrendPoint(
                        row[0] != null ? row[0].toString() : "",
                        row[1] != null ? new BigDecimal(row[1].toString()).doubleValue() : 0.0))
                .toList();

        List<NameValuePoint> statusDistribution = labOrderRepo.statusDistribution(fromInstant, toInstant)
                .stream()
                .map(row -> new NameValuePoint(
                        row[0] != null ? row[0].toString() : "Unknown",
                        row[1] != null ? ((Number) row[1]).doubleValue() : 0.0))
                .toList();

        // byDepartmentReferral maps to source_type (OPD / IPD / WALK_IN)
        List<NameValuePoint> byDeptReferral = labOrderRepo.byDepartmentReferral(fromInstant, toInstant)
                .stream()
                .map(row -> new NameValuePoint(
                        row[0] != null ? row[0].toString() : "Unknown",
                        row[1] != null ? ((Number) row[1]).doubleValue() : 0.0))
                .toList();

        return new LaboratoryAnalyticsResponse(
                totalTests,
                totalRev,
                pendingReports,
                dailyTestsTrend,
                topTests,
                revenueTrend,
                statusDistribution,
                byDeptReferral
        );
    }

    /** Called by Executive Dashboard */
    public long getLabTestsToday() {
        return labOrderRepo.countTestsToday();
    }
}
