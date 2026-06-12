package com.smarthospital.modules.inventory.service;

import com.smarthospital.modules.analytics.dto.InventoryAnalyticsResponse;
import com.smarthospital.modules.analytics.dto.InventoryAnalyticsResponse.LowStockEntry;
import com.smarthospital.modules.analytics.dto.NameValuePoint;
import com.smarthospital.modules.analytics.dto.TrendPoint;
import com.smarthospital.modules.inventory.domain.InventoryItem;
import com.smarthospital.modules.inventory.repository.InventoryItemRepository;
import com.smarthospital.modules.inventory.repository.StockIssueRepository;
import com.smarthospital.modules.inventory.repository.StockReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryAnalyticsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d");

    private final InventoryItemRepository itemRepo;
    private final StockReceiptRepository receiptRepo;
    private final StockIssueRepository issueRepo;

    public InventoryAnalyticsService(InventoryItemRepository itemRepo,
                                     StockReceiptRepository receiptRepo,
                                     StockIssueRepository issueRepo) {
        this.itemRepo = itemRepo;
        this.receiptRepo = receiptRepo;
        this.issueRepo = issueRepo;
    }

    public InventoryAnalyticsResponse getAnalytics(LocalDate from, LocalDate to) {
        // Scalar KPIs
        BigDecimal totalStockValue = receiptRepo.totalStockValue();
        if (totalStockValue == null) totalStockValue = BigDecimal.ZERO;

        long lowStockItems   = itemRepo.countLowStock();
        long outOfStockItems = itemRepo.countOutOfStock();
        long totalItems      = itemRepo.count();

        // Stock value trend — daily receipt totals as a proxy
        List<Object[]> dailyRaw = receiptRepo.dailyReceiptTotals(from, to);
        List<TrendPoint> stockValueTrend = dailyRaw.stream()
                .map(row -> new TrendPoint(
                        LocalDate.parse(row[0].toString()).format(DATE_FMT),
                        row[1] == null ? 0.0 : ((Number) row[1]).doubleValue()))
                .toList();

        // Fast-moving items — top 10 by issued quantity in period
        List<Object[]> fastRaw = issueRepo.topIssuedItems(from, to, 10);
        List<NameValuePoint> fastMovingItems = fastRaw.stream()
                .map(row -> new NameValuePoint(
                        row[0].toString(),
                        row[1] == null ? 0.0 : ((Number) row[1]).doubleValue()))
                .toList();

        // Slow-moving items — top 10 non-zero-stock items with lowest issue quantities
        List<Object[]> slowRaw = issueRepo.slowMovingItems(from, to, 10);
        List<NameValuePoint> slowMovingItems = slowRaw.stream()
                .map(row -> new NameValuePoint(
                        row[0].toString(),
                        row[1] == null ? 0.0 : ((Number) row[1]).doubleValue()))
                .toList();

        // Stock by category
        List<Object[]> catRaw = itemRepo.stockByCategoryRaw();
        List<NameValuePoint> stockByCategory = catRaw.stream()
                .map(row -> new NameValuePoint(
                        row[0].toString(),
                        row[1] == null ? 0.0 : ((Number) row[1]).doubleValue()))
                .toList();

        // Low stock list — detailed entries
        List<LowStockEntry> lowStockList = itemRepo.findLowStockItems().stream()
                .map(item -> new LowStockEntry(
                        item.getName(),
                        item.getCategoryName(),
                        item.getCurrentStock(),
                        item.getReorderLevel()))
                .toList();

        return new InventoryAnalyticsResponse(
                totalStockValue,
                lowStockItems,
                outOfStockItems,
                totalItems,
                stockValueTrend,
                fastMovingItems,
                slowMovingItems,
                stockByCategory,
                lowStockList
        );
    }

    /** Count of items currently at or below their reorder level. */
    public long getInventoryAlerts() {
        return itemRepo.countLowStock();
    }
}
