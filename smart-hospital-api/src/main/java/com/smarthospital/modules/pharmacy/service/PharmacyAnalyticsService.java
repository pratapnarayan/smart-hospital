package com.smarthospital.modules.pharmacy.service;

import com.smarthospital.modules.analytics.dto.NameValuePoint;
import com.smarthospital.modules.analytics.dto.PharmacyAnalyticsResponse;
import com.smarthospital.modules.analytics.dto.TrendPoint;
import com.smarthospital.modules.pharmacy.repository.MedicineBatchRepository;
import com.smarthospital.modules.pharmacy.repository.PharmacyBillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PharmacyAnalyticsService {

    private final PharmacyBillRepository billRepo;
    private final MedicineBatchRepository batchRepo;

    public PharmacyAnalyticsService(PharmacyBillRepository billRepo,
                                    MedicineBatchRepository batchRepo) {
        this.billRepo  = billRepo;
        this.batchRepo = batchRepo;
    }

    public PharmacyAnalyticsResponse getAnalytics(LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Revenue & bill count
        BigDecimal totalRevenue = billRepo.sumNetAmountBetween(fromInstant, toInstant);
        long totalBills         = billRepo.countBetween(fromInstant, toInstant);

        // Stock alerts
        long lowStock    = batchRepo.countLowStockBatches();
        long expiringSoon = batchRepo.countExpiringBatches(LocalDate.now(),
                                                            LocalDate.now().plusDays(30));

        // Top medicines by revenue
        List<NameValuePoint> topMedicines = billRepo.topMedicinesByRevenue(fromInstant, toInstant)
                .stream()
                .map(row -> new NameValuePoint(
                        row[0] != null ? row[0].toString() : "Unknown",
                        row[1] != null ? new BigDecimal(row[1].toString()).doubleValue() : 0.0))
                .toList();

        // Stock health distribution
        List<NameValuePoint> stockHealth = batchRepo.stockHealthDistribution()
                .stream()
                .map(row -> new NameValuePoint(
                        row[0] != null ? row[0].toString() : "Unknown",
                        row[1] != null ? ((Number) row[1]).doubleValue() : 0.0))
                .toList();

        // Revenue by category (batch_id may be null for deleted batches — those rows are excluded)
        List<NameValuePoint> revByCategory = billRepo.revenueByCategory(fromInstant, toInstant)
                .stream()
                .map(row -> new NameValuePoint(
                        row[0] != null ? row[0].toString() : "Unknown",
                        row[1] != null ? new BigDecimal(row[1].toString()).doubleValue() : 0.0))
                .toList();

        // Daily revenue trend
        List<TrendPoint> revenueTrend = billRepo.dailyRevenueTrend(fromInstant, toInstant)
                .stream()
                .map(row -> new TrendPoint(
                        row[0] != null ? row[0].toString() : "",
                        row[1] != null ? new BigDecimal(row[1].toString()).doubleValue() : 0.0))
                .toList();

        return new PharmacyAnalyticsResponse(
                totalRevenue,
                totalBills,
                lowStock,
                expiringSoon,
                topMedicines,
                stockHealth,
                revByCategory,
                revenueTrend
        );
    }

    /** Called by Executive Dashboard */
    public BigDecimal getMedicineSalesToday() {
        return billRepo.sumNetAmountToday();
    }

    /** Called by Executive Dashboard */
    public long getLowStockAlerts() {
        return batchRepo.countLowStockBatches();
    }
}
