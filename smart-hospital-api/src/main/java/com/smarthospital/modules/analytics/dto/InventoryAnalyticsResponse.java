package com.smarthospital.modules.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record InventoryAnalyticsResponse(
    BigDecimal totalStockValue,
    long lowStockItems,
    long outOfStockItems,
    long totalItems,

    List<TrendPoint> stockValueTrend,
    List<NameValuePoint> fastMovingItems,
    List<NameValuePoint> slowMovingItems,
    List<NameValuePoint> stockByCategory,
    List<LowStockEntry> lowStockList
) {
    public record LowStockEntry(String itemName, String category, long currentStock, long reorderLevel) {}
}
