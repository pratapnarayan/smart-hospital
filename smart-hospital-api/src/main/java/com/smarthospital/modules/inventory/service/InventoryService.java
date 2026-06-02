package com.smarthospital.modules.inventory.service;

import com.smarthospital.core.exception.ApiException;
import com.smarthospital.core.pagination.PageResponse;
import com.smarthospital.modules.inventory.domain.ItemCategory;
import com.smarthospital.modules.inventory.domain.InventoryItem;
import com.smarthospital.modules.inventory.domain.StockReceipt;
import com.smarthospital.modules.inventory.domain.StockIssue;
import com.smarthospital.modules.inventory.dto.*;
import com.smarthospital.modules.inventory.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ItemCategoryRepository  categoryRepository;
    private final InventoryItemRepository itemRepository;
    private final StockReceiptRepository  receiptRepository;
    private final StockIssueRepository    issueRepository;

    public InventoryService(ItemCategoryRepository  categoryRepository,
                            InventoryItemRepository itemRepository,
                            StockReceiptRepository  receiptRepository,
                            StockIssueRepository    issueRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository     = itemRepository;
        this.receiptRepository  = receiptRepository;
        this.issueRepository    = issueRepository;
    }

    // ── Categories ────────────────────────────────────────────────────────────

    public List<ItemCategoryResponse> listCategories() {
        return categoryRepository.findByActiveTrue().stream()
                .map(ItemCategoryResponse::from).toList();
    }

    @Transactional
    public ItemCategoryResponse createCategory(ItemCategoryRequest req) {
        if (categoryRepository.existsByNameIgnoreCase(req.name()))
            throw ApiException.conflict("CATEGORY_EXISTS",
                    "Category '" + req.name() + "' already exists");
        return ItemCategoryResponse.from(categoryRepository.save(
                ItemCategory.builder().name(req.name()).description(req.description()).build()));
    }

    // ── Items ─────────────────────────────────────────────────────────────────

    public PageResponse<InventoryItemResponse> listItems(
            String q, UUID categoryId, Boolean lowStock, Pageable pageable) {
        if (Boolean.TRUE.equals(lowStock))
            return PageResponse.of(itemRepository.findLowStockItemsPaged(pageable)
                    .map(InventoryItemResponse::from));
        if (StringUtils.hasText(q))
            return PageResponse.of(itemRepository.search(q, pageable)
                    .map(InventoryItemResponse::from));
        if (categoryId != null)
            return PageResponse.of(itemRepository.findByCategoryId(categoryId, pageable)
                    .map(InventoryItemResponse::from));
        return PageResponse.of(itemRepository.findAll(pageable)
                .map(InventoryItemResponse::from));
    }

    public InventoryItemResponse getItem(UUID id) {
        return InventoryItemResponse.from(findItemOrThrow(id));
    }

    @Transactional
    public InventoryItemResponse createItem(InventoryItemRequest req) {
        if (itemRepository.existsByItemCodeIgnoreCase(req.itemCode()))
            throw ApiException.conflict("ITEM_CODE_EXISTS",
                    "Item code '" + req.itemCode() + "' already in use");
        ItemCategory category = findCategoryOrThrow(req.categoryId());
        InventoryItem item = InventoryItem.builder()
                .itemCode(req.itemCode().toUpperCase())
                .name(req.name())
                .description(req.description())
                .categoryId(category.getId())
                .categoryName(category.getName())
                .unit(req.unit())
                .reorderLevel(req.reorderLevel())
                .build();
        return InventoryItemResponse.from(itemRepository.save(item));
    }

    @Transactional
    public InventoryItemResponse updateItem(UUID id, InventoryItemRequest req) {
        InventoryItem item = findItemOrThrow(id);
        if (!item.getItemCode().equalsIgnoreCase(req.itemCode()) &&
                itemRepository.existsByItemCodeIgnoreCaseAndIdNot(req.itemCode(), id))
            throw ApiException.conflict("ITEM_CODE_EXISTS",
                    "Item code '" + req.itemCode() + "' already in use");
        ItemCategory category = findCategoryOrThrow(req.categoryId());
        item.setItemCode(req.itemCode().toUpperCase());
        item.setName(req.name());
        item.setDescription(req.description());
        item.setCategoryId(category.getId());
        item.setCategoryName(category.getName());
        item.setUnit(req.unit());
        item.setReorderLevel(req.reorderLevel());
        return InventoryItemResponse.from(itemRepository.save(item));
    }

    // ── Stock Receipts ────────────────────────────────────────────────────────

    @Transactional
    public StockReceiptResponse recordReceipt(StockReceiptRequest req) {
        InventoryItem item = findItemOrThrow(req.itemId());
        BigDecimal unitCost  = req.unitCost() != null ? req.unitCost() : BigDecimal.ZERO;
        BigDecimal totalCost = unitCost.multiply(BigDecimal.valueOf(req.quantity()));

        StockReceipt receipt = StockReceipt.builder()
                .receiptNumber(generateReceiptNumber())
                .entryDate(req.entryDate() != null ? req.entryDate() : LocalDate.now())
                .itemId(item.getId())
                .itemName(item.getName())
                .itemUnit(item.getUnit())
                .quantity(req.quantity())
                .unitCost(unitCost)
                .totalCost(totalCost)
                .supplierName(req.supplierName())
                .grnNumber(req.grnNumber())
                .receivedBy(req.receivedBy())
                .notes(req.notes())
                .build();

        item.setCurrentStock(item.getCurrentStock() + req.quantity());
        itemRepository.save(item);

        StockReceipt saved = receiptRepository.save(receipt);
        log.info("Stock receipt {} — received {} {} of {}",
                saved.getReceiptNumber(), req.quantity(), item.getUnit(), item.getName());
        return StockReceiptResponse.from(saved);
    }

    public StockReceiptResponse getReceipt(UUID id) {
        return receiptRepository.findById(id)
                .map(StockReceiptResponse::from)
                .orElseThrow(() -> ApiException.notFound("RECEIPT_NOT_FOUND",
                        "Receipt " + id + " not found"));
    }

    public PageResponse<StockReceiptResponse> listReceipts(
            LocalDate from, LocalDate to, UUID itemId, Pageable pageable) {
        LocalDate f = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate t = to   != null ? to   : LocalDate.now();
        if (itemId != null)
            return PageResponse.of(receiptRepository
                    .findByItemIdAndEntryDateBetween(itemId, f, t, pageable)
                    .map(StockReceiptResponse::from));
        return PageResponse.of(receiptRepository
                .findByEntryDateBetween(f, t, pageable)
                .map(StockReceiptResponse::from));
    }

    // ── Stock Issues ──────────────────────────────────────────────────────────

    @Transactional
    public StockIssueResponse recordIssue(StockIssueRequest req) {
        InventoryItem item = findItemOrThrow(req.itemId());
        if (item.getCurrentStock() < req.quantity())
            throw ApiException.badRequest("INSUFFICIENT_STOCK",
                    "Available: " + item.getCurrentStock() + " " + item.getUnit() +
                    ", requested: " + req.quantity());

        StockIssue issue = StockIssue.builder()
                .issueNumber(generateIssueNumber())
                .issueDate(req.issueDate() != null ? req.issueDate() : LocalDate.now())
                .itemId(item.getId())
                .itemName(item.getName())
                .itemUnit(item.getUnit())
                .quantity(req.quantity())
                .issuedTo(req.issuedTo())
                .issuedBy(req.issuedBy())
                .purpose(req.purpose())
                .notes(req.notes())
                .build();

        item.setCurrentStock(item.getCurrentStock() - req.quantity());
        itemRepository.save(item);

        if (item.isLowStock())
            log.warn("Low stock — {}: {} {} remaining (reorder @ {})",
                    item.getName(), item.getCurrentStock(), item.getUnit(), item.getReorderLevel());

        StockIssue saved = issueRepository.save(issue);
        log.info("Stock issue {} — issued {} {} of {} to {}",
                saved.getIssueNumber(), req.quantity(), item.getUnit(), item.getName(), req.issuedTo());
        return StockIssueResponse.from(saved);
    }

    public StockIssueResponse getIssue(UUID id) {
        return issueRepository.findById(id)
                .map(StockIssueResponse::from)
                .orElseThrow(() -> ApiException.notFound("ISSUE_NOT_FOUND",
                        "Issue " + id + " not found"));
    }

    public PageResponse<StockIssueResponse> listIssues(
            LocalDate from, LocalDate to, UUID itemId, Pageable pageable) {
        LocalDate f = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate t = to   != null ? to   : LocalDate.now();
        if (itemId != null)
            return PageResponse.of(issueRepository
                    .findByItemIdAndIssueDateBetween(itemId, f, t, pageable)
                    .map(StockIssueResponse::from));
        return PageResponse.of(issueRepository
                .findByIssueDateBetween(f, t, pageable)
                .map(StockIssueResponse::from));
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    public InventoryDashboardResponse getDashboard() {
        List<InventoryItem> lowStockItems = itemRepository.findLowStockItems();
        return new InventoryDashboardResponse(
                itemRepository.count(),
                lowStockItems.size(),
                receiptRepository.countToday(),
                issueRepository.countToday(),
                receiptRepository.sumTodayCost(),
                lowStockItems.stream().map(InventoryItemResponse::from).toList()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private InventoryItem findItemOrThrow(UUID id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ITEM_NOT_FOUND",
                        "Inventory item " + id + " not found"));
    }

    private ItemCategory findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CATEGORY_NOT_FOUND",
                        "Item category " + id + " not found"));
    }

    private String generateReceiptNumber() {
        int year = LocalDate.now().getYear();
        long seq = receiptRepository.nextSequenceForYear(year);
        return String.format("GR-%d-%05d", year, seq);
    }

    private String generateIssueNumber() {
        int year = LocalDate.now().getYear();
        long seq = issueRepository.nextSequenceForYear(year);
        return String.format("ISS-%d-%05d", year, seq);
    }
}
