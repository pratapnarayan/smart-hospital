package com.smarthospital.modules.finance.service;

import com.smarthospital.modules.analytics.dto.*;
import com.smarthospital.modules.finance.repository.ExpenseEntryRepository;
import com.smarthospital.modules.finance.repository.IncomeEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FinanceAnalyticsService {

    private final IncomeEntryRepository incomeRepo;
    private final ExpenseEntryRepository expenseRepo;

    public FinanceAnalyticsService(IncomeEntryRepository incomeRepo,
                                   ExpenseEntryRepository expenseRepo) {
        this.incomeRepo = incomeRepo;
        this.expenseRepo = expenseRepo;
    }

    public FinanceAnalyticsResponse getAnalytics(LocalDate from, LocalDate to) {
        BigDecimal totalRevenue = incomeRepo.sumBetween(from, to);
        BigDecimal totalExpenses = expenseRepo.sumBetween(from, to);
        BigDecimal netProfit = totalRevenue.subtract(totalExpenses);

        // Collection efficiency: revenue / (revenue + expenses) * 100
        double efficiency = 0;
        BigDecimal total = totalRevenue.add(totalExpenses);
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            efficiency = totalRevenue.divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        List<TrendPoint> dailyRevenue = buildDailyRevenueTrend(from, to);
        List<NameValuePoint> bySource = buildRevenueBySource(from, to);
        List<NameValuePoint> byDoctor = buildRevenueByDoctor(from, to);
        List<NameValuePoint> monthly = buildMonthlyComparison(from, to);
        List<TrendPoint> expenseTrend = buildDailyExpenseTrend(from, to);

        return new FinanceAnalyticsResponse(
                totalRevenue, totalExpenses, netProfit, efficiency,
                dailyRevenue, bySource, byDoctor, monthly, expenseTrend);
    }

    // Called by ExecutiveDashboardService
    public BigDecimal getTodayRevenue() {
        return incomeRepo.sumByDate(LocalDate.now());
    }

    public BigDecimal getMonthRevenue() {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        return incomeRepo.sumBetween(start, LocalDate.now());
    }

    public BigDecimal getPendingPayments() {
        // Approximation: sum of this month's expenses not yet covered by income
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        BigDecimal income = incomeRepo.sumBetween(start, LocalDate.now());
        BigDecimal expenses = expenseRepo.sumBetween(start, LocalDate.now());
        BigDecimal pending = expenses.subtract(income);
        return pending.compareTo(BigDecimal.ZERO) > 0 ? pending : BigDecimal.ZERO;
    }

    private List<TrendPoint> buildDailyRevenueTrend(LocalDate from, LocalDate to) {
        List<TrendPoint> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            BigDecimal amt = incomeRepo.sumByDate(d);
            trend.add(new TrendPoint(d.format(fmt), amt.doubleValue()));
        }
        return trend;
    }

    private List<TrendPoint> buildDailyExpenseTrend(LocalDate from, LocalDate to) {
        List<TrendPoint> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            BigDecimal amt = expenseRepo.sumByDate(d);
            trend.add(new TrendPoint(d.format(fmt), amt.doubleValue()));
        }
        return trend;
    }

    private List<NameValuePoint> buildRevenueBySource(LocalDate from, LocalDate to) {
        List<Object[]> rows = incomeRepo.sumBySourceType(from, to);
        List<NameValuePoint> result = new ArrayList<>();
        for (Object[] row : rows) {
            String source = row[0] != null ? row[0].toString() : "OTHER";
            double amount = row[1] != null ? ((Number) row[1]).doubleValue() : 0;
            result.add(new NameValuePoint(source, amount));
        }
        return result;
    }

    private List<NameValuePoint> buildRevenueByDoctor(LocalDate from, LocalDate to) {
        return incomeRepo.sumByDoctorName(from, to).stream()
                .map(row -> new NameValuePoint(
                        row[0] != null ? row[0].toString() : "Unknown",
                        row[1] != null ? ((Number) row[1]).doubleValue() : 0))
                .limit(10)
                .toList();
    }

    private List<NameValuePoint> buildMonthlyComparison(LocalDate from, LocalDate to) {
        return incomeRepo.sumByMonth(from, to).stream()
                .map(row -> new NameValuePoint(
                        row[0] != null ? row[0].toString() : "Unknown",
                        row[1] != null ? ((Number) row[1]).doubleValue() : 0))
                .toList();
    }
}
