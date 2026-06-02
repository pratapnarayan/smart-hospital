package com.smarthospital.modules.finance.service;

import com.smarthospital.core.exception.ApiException;
import com.smarthospital.core.pagination.PageResponse;
import com.smarthospital.modules.finance.domain.ExpenseCategory;
import com.smarthospital.modules.finance.domain.ExpenseEntry;
import com.smarthospital.modules.finance.domain.IncomeEntry;
import com.smarthospital.modules.finance.domain.IncomeEntry.SourceType;
import com.smarthospital.modules.finance.dto.*;
import com.smarthospital.modules.finance.repository.ExpenseCategoryRepository;
import com.smarthospital.modules.finance.repository.ExpenseEntryRepository;
import com.smarthospital.modules.finance.repository.IncomeEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FinanceService {

    private static final Logger log = LoggerFactory.getLogger(FinanceService.class);

    private final ExpenseCategoryRepository categoryRepository;
    private final IncomeEntryRepository     incomeRepository;
    private final ExpenseEntryRepository    expenseRepository;

    public FinanceService(ExpenseCategoryRepository categoryRepository,
                          IncomeEntryRepository     incomeRepository,
                          ExpenseEntryRepository    expenseRepository) {
        this.categoryRepository = categoryRepository;
        this.incomeRepository   = incomeRepository;
        this.expenseRepository  = expenseRepository;
    }

    // ── Expense Categories ────────────────────────────────────────────────────

    public List<ExpenseCategoryResponse> listCategories() {
        return categoryRepository.findByActiveTrue().stream()
                .map(ExpenseCategoryResponse::from).toList();
    }

    @Transactional
    public ExpenseCategoryResponse createCategory(ExpenseCategoryRequest req) {
        if (categoryRepository.existsByNameIgnoreCase(req.name()))
            throw ApiException.conflict("CATEGORY_EXISTS",
                    "Expense category '" + req.name() + "' already exists");
        return ExpenseCategoryResponse.from(categoryRepository.save(
                ExpenseCategory.builder().name(req.name()).description(req.description()).build()));
    }

    // ── Income Entries ────────────────────────────────────────────────────────

    @Transactional
    public IncomeEntryResponse createIncome(IncomeEntryRequest req) {
        LocalDate date = req.entryDate() != null ? req.entryDate() : LocalDate.now();
        IncomeEntry entry = IncomeEntry.builder()
                .entryNumber(generateIncomeNumber())
                .entryDate(date)
                .sourceType(req.sourceType())
                .sourceId(req.sourceId())
                .patientName(req.patientName())
                .amount(req.amount())
                .description(req.description())
                .paymentMode(req.paymentMode())
                .referenceNo(req.referenceNo())
                .receivedBy(req.receivedBy())
                .notes(req.notes())
                .build();
        IncomeEntry saved = incomeRepository.save(entry);
        log.info("Income entry {} created: {} via {}", saved.getEntryNumber(),
                saved.getAmount(), saved.getSourceType());
        return IncomeEntryResponse.from(saved);
    }

    public IncomeEntryResponse getIncome(UUID id) {
        return IncomeEntryResponse.from(findIncomeOrThrow(id));
    }

    public PageResponse<IncomeEntryResponse> listIncome(
            LocalDate from, LocalDate to, SourceType sourceType, Pageable pageable) {
        LocalDate f = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate t = to   != null ? to   : LocalDate.now();
        if (sourceType != null)
            return PageResponse.of(incomeRepository
                    .findByEntryDateBetweenAndSourceType(f, t, sourceType, pageable)
                    .map(IncomeEntryResponse::from));
        return PageResponse.of(incomeRepository
                .findByEntryDateBetween(f, t, pageable)
                .map(IncomeEntryResponse::from));
    }

    // ── Expense Entries ───────────────────────────────────────────────────────

    @Transactional
    public ExpenseEntryResponse createExpense(ExpenseEntryRequest req) {
        ExpenseCategory category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> ApiException.notFound("CATEGORY_NOT_FOUND",
                        "Expense category " + req.categoryId() + " not found"));
        LocalDate date = req.entryDate() != null ? req.entryDate() : LocalDate.now();
        ExpenseEntry entry = ExpenseEntry.builder()
                .entryNumber(generateExpenseNumber())
                .entryDate(date)
                .categoryId(category.getId())
                .categoryName(category.getName())
                .description(req.description())
                .amount(req.amount())
                .paymentMode(req.paymentMode())
                .referenceNo(req.referenceNo())
                .paidTo(req.paidTo())
                .approvedBy(req.approvedBy())
                .notes(req.notes())
                .build();
        ExpenseEntry saved = expenseRepository.save(entry);
        log.info("Expense entry {} created: {} for {}", saved.getEntryNumber(),
                saved.getAmount(), saved.getCategoryName());
        return ExpenseEntryResponse.from(saved);
    }

    public ExpenseEntryResponse getExpense(UUID id) {
        return ExpenseEntryResponse.from(findExpenseOrThrow(id));
    }

    public PageResponse<ExpenseEntryResponse> listExpenses(
            LocalDate from, LocalDate to, UUID categoryId, Pageable pageable) {
        LocalDate f = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate t = to   != null ? to   : LocalDate.now();
        if (categoryId != null)
            return PageResponse.of(expenseRepository
                    .findByEntryDateBetweenAndCategoryId(f, t, categoryId, pageable)
                    .map(ExpenseEntryResponse::from));
        return PageResponse.of(expenseRepository
                .findByEntryDateBetween(f, t, pageable)
                .map(ExpenseEntryResponse::from));
    }

    // ── Dashboard & Reports ───────────────────────────────────────────────────

    public FinanceDashboardResponse getDashboard() {
        LocalDate today      = LocalDate.now();
        // Rolling 30-day window so the dashboard always shows data even on day 1 of a new month.
        LocalDate monthStart = today.minusDays(29);

        BigDecimal todayIncome   = incomeRepository.sumByDate(today);
        BigDecimal todayExpenses = expenseRepository.sumByDate(today);
        BigDecimal monthIncome   = incomeRepository.sumBetween(monthStart, today);
        BigDecimal monthExpenses = expenseRepository.sumBetween(monthStart, today);

        List<FinanceDashboardResponse.SourceBreakdown> bySource =
                incomeRepository.sumBySourceType(monthStart, today).stream()
                        .map(r -> new FinanceDashboardResponse.SourceBreakdown(
                                (String) r[0], toBigDecimal(r[1])))
                        .toList();

        List<FinanceDashboardResponse.CategoryBreakdown> byCategory =
                expenseRepository.sumByCategoryName(monthStart, today).stream()
                        .map(r -> new FinanceDashboardResponse.CategoryBreakdown(
                                (String) r[0], toBigDecimal(r[1])))
                        .toList();

        return new FinanceDashboardResponse(
                todayIncome,
                todayExpenses,
                todayIncome.subtract(todayExpenses),
                monthIncome,
                monthExpenses,
                monthIncome.subtract(monthExpenses),
                bySource,
                byCategory
        );
    }

    public PeriodSummaryResponse getSummary(LocalDate from, LocalDate to) {
        LocalDate f = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate t = to   != null ? to   : LocalDate.now();
        BigDecimal income   = incomeRepository.sumBetween(f, t);
        BigDecimal expenses = expenseRepository.sumBetween(f, t);
        return new PeriodSummaryResponse(f, t, income, expenses, income.subtract(expenses));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private IncomeEntry findIncomeOrThrow(UUID id) {
        return incomeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ENTRY_NOT_FOUND",
                        "Income entry " + id + " not found"));
    }

    private ExpenseEntry findExpenseOrThrow(UUID id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ENTRY_NOT_FOUND",
                        "Expense entry " + id + " not found"));
    }

    private String generateIncomeNumber() {
        int year  = LocalDate.now().getYear();
        long seq  = incomeRepository.nextSequenceForYear(year);
        return String.format("INC-%d-%05d", year, seq);
    }

    private String generateExpenseNumber() {
        int year  = LocalDate.now().getYear();
        long seq  = expenseRepository.nextSequenceForYear(year);
        return String.format("EXP-%d-%05d", year, seq);
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd;
        return new BigDecimal(val.toString());
    }
}
