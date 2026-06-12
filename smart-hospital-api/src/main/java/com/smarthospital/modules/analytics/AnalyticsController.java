package com.smarthospital.modules.analytics;

import com.smarthospital.core.export.ExcelExportUtil;
import com.smarthospital.core.export.PdfExportUtil;
import com.smarthospital.core.security.UserPrincipal;
import com.smarthospital.modules.analytics.dto.*;
import com.smarthospital.modules.auth.domain.Permission;
import com.smarthospital.modules.doctor.service.DoctorAnalyticsService;
import com.smarthospital.modules.finance.service.FinanceAnalyticsService;
import com.smarthospital.modules.frontoffice.service.AppointmentAnalyticsService;
import com.smarthospital.modules.inventory.service.InventoryAnalyticsService;
import com.smarthospital.modules.pathology.service.PathologyAnalyticsService;
import com.smarthospital.modules.patient.service.PatientAnalyticsService;
import com.smarthospital.modules.pharmacy.service.PharmacyAnalyticsService;
import com.smarthospital.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Reports and analytics dashboards")
public class AnalyticsController {

    private final ExecutiveDashboardService executiveDashboardService;
    private final FinanceAnalyticsService financeAnalytics;
    private final PatientAnalyticsService patientAnalytics;
    private final DoctorAnalyticsService doctorAnalytics;
    private final AppointmentAnalyticsService appointmentAnalytics;
    private final PharmacyAnalyticsService pharmacyAnalytics;
    private final PathologyAnalyticsService pathologyAnalytics;
    private final InventoryAnalyticsService inventoryAnalytics;

    public AnalyticsController(
            ExecutiveDashboardService executiveDashboardService,
            FinanceAnalyticsService financeAnalytics,
            PatientAnalyticsService patientAnalytics,
            DoctorAnalyticsService doctorAnalytics,
            AppointmentAnalyticsService appointmentAnalytics,
            PharmacyAnalyticsService pharmacyAnalytics,
            PathologyAnalyticsService pathologyAnalytics,
            InventoryAnalyticsService inventoryAnalytics) {
        this.executiveDashboardService = executiveDashboardService;
        this.financeAnalytics = financeAnalytics;
        this.patientAnalytics = patientAnalytics;
        this.doctorAnalytics = doctorAnalytics;
        this.appointmentAnalytics = appointmentAnalytics;
        this.pharmacyAnalytics = pharmacyAnalytics;
        this.pathologyAnalytics = pathologyAnalytics;
        this.inventoryAnalytics = inventoryAnalytics;
    }

    @GetMapping("/executive")
    @PreAuthorize("hasAuthority('" + Permission.REPORTS_VIEW + "')")
    @Operation(summary = "Executive dashboard — aggregated KPIs and charts")
    public ResponseEntity<ApiResponse<ExecutiveDashboardResponse>> executive() {
        return ResponseEntity.ok(ApiResponse.ok(executiveDashboardService.getDashboard()));
    }

    @GetMapping("/finance")
    @PreAuthorize("hasAuthority('" + Permission.REPORTS_VIEW + "')")
    @Operation(summary = "Financial analytics for a date range")
    public ResponseEntity<ApiResponse<FinanceAnalyticsResponse>> finance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        return ResponseEntity.ok(ApiResponse.ok(financeAnalytics.getAnalytics(range[0], range[1])));
    }

    @GetMapping("/patients")
    @PreAuthorize("hasAuthority('" + Permission.REPORTS_VIEW + "')")
    @Operation(summary = "Patient analytics for a date range")
    public ResponseEntity<ApiResponse<PatientAnalyticsResponse>> patients(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        return ResponseEntity.ok(ApiResponse.ok(patientAnalytics.getAnalytics(range[0], range[1])));
    }

    @GetMapping("/doctors")
    @PreAuthorize("hasAuthority('" + Permission.REPORTS_VIEW + "')")
    @Operation(summary = "Doctor analytics for a date range")
    public ResponseEntity<ApiResponse<DoctorAnalyticsResponse>> doctors(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        return ResponseEntity.ok(ApiResponse.ok(doctorAnalytics.getAnalytics(range[0], range[1])));
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasAuthority('" + Permission.REPORTS_VIEW + "')")
    @Operation(summary = "Appointment analytics for a date range")
    public ResponseEntity<ApiResponse<AppointmentAnalyticsResponse>> appointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        return ResponseEntity.ok(ApiResponse.ok(appointmentAnalytics.getAnalytics(range[0], range[1])));
    }

    @GetMapping("/pharmacy")
    @PreAuthorize("hasAuthority('" + Permission.REPORTS_VIEW + "')")
    @Operation(summary = "Pharmacy analytics for a date range")
    public ResponseEntity<ApiResponse<PharmacyAnalyticsResponse>> pharmacy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        return ResponseEntity.ok(ApiResponse.ok(pharmacyAnalytics.getAnalytics(range[0], range[1])));
    }

    @GetMapping("/laboratory")
    @PreAuthorize("hasAuthority('" + Permission.REPORTS_VIEW + "')")
    @Operation(summary = "Laboratory analytics for a date range")
    public ResponseEntity<ApiResponse<LaboratoryAnalyticsResponse>> laboratory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        return ResponseEntity.ok(ApiResponse.ok(pathologyAnalytics.getAnalytics(range[0], range[1])));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAuthority('" + Permission.REPORTS_VIEW + "')")
    @Operation(summary = "Inventory analytics for a date range")
    public ResponseEntity<ApiResponse<InventoryAnalyticsResponse>> inventory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        return ResponseEntity.ok(ApiResponse.ok(inventoryAnalytics.getAnalytics(range[0], range[1])));
    }

    @GetMapping("/export/{section}")
    @PreAuthorize("hasAuthority('" + Permission.REPORTS_VIEW + "')")
    @Operation(summary = "Export analytics section as Excel or PDF")
    public ResponseEntity<byte[]> export(
            @PathVariable String section,
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal user) {

        LocalDate[] range = resolveRange(from, to);
        String generatedBy = user != null ? user.getUsername() : "System";
        String title = section.substring(0, 1).toUpperCase() + section.substring(1) + " Analytics";
        String dateRange = range[0] + " to " + range[1];

        byte[] data;
        String mediaType;
        String filename;

        if ("pdf".equalsIgnoreCase(format)) {
            data = buildRichPdf(section, range[0], range[1], dateRange, generatedBy);
            mediaType = "application/pdf";
            filename = section + "-analytics.pdf";
        } else {
            data = buildRichExcel(section, range[0], range[1], dateRange, generatedBy);
            mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            filename = section + "-analytics.xlsx";
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", mediaType)
                .body(data);
    }

    private byte[] buildRichExcel(String section, LocalDate from, LocalDate to,
                                  String dateRange, String generatedBy) {
        return switch (section.toLowerCase()) {
            case "executive"    -> ExcelExportUtil.buildExecutive(
                    executiveDashboardService.getDashboard(), dateRange, generatedBy);
            case "finance"      -> ExcelExportUtil.buildFinance(
                    financeAnalytics.getAnalytics(from, to), dateRange, generatedBy);
            case "patients"     -> ExcelExportUtil.buildPatients(
                    patientAnalytics.getAnalytics(from, to), dateRange, generatedBy);
            case "doctors"      -> ExcelExportUtil.buildDoctors(
                    doctorAnalytics.getAnalytics(from, to), dateRange, generatedBy);
            case "appointments" -> ExcelExportUtil.buildAppointments(
                    appointmentAnalytics.getAnalytics(from, to), dateRange, generatedBy);
            case "pharmacy"     -> ExcelExportUtil.buildPharmacy(
                    pharmacyAnalytics.getAnalytics(from, to), dateRange, generatedBy);
            case "laboratory"   -> ExcelExportUtil.buildLaboratory(
                    pathologyAnalytics.getAnalytics(from, to), dateRange, generatedBy);
            case "inventory"    -> ExcelExportUtil.buildInventory(
                    inventoryAnalytics.getAnalytics(from, to), dateRange, generatedBy);
            default             -> ExcelExportUtil.buildExecutive(
                    executiveDashboardService.getDashboard(), dateRange, generatedBy);
        };
    }

    private byte[] buildRichPdf(String section, LocalDate from, LocalDate to,
                                String dateRange, String generatedBy) {
        return switch (section.toLowerCase()) {
            case "executive" -> PdfExportUtil.buildExecutive(
                    executiveDashboardService.getDashboard(), dateRange, generatedBy);
            case "finance" -> PdfExportUtil.buildFinance(
                    financeAnalytics.getAnalytics(from, to), dateRange, generatedBy);
            case "patients" -> PdfExportUtil.buildPatients(
                    patientAnalytics.getAnalytics(from, to), dateRange, generatedBy);
            case "doctors" -> PdfExportUtil.buildDoctors(
                    doctorAnalytics.getAnalytics(from, to), dateRange, generatedBy);
            case "appointments" -> PdfExportUtil.buildAppointments(
                    appointmentAnalytics.getAnalytics(from, to), dateRange, generatedBy);
            case "pharmacy" -> PdfExportUtil.buildPharmacy(
                    pharmacyAnalytics.getAnalytics(from, to), dateRange, generatedBy);
            case "laboratory" -> PdfExportUtil.buildLaboratory(
                    pathologyAnalytics.getAnalytics(from, to), dateRange, generatedBy);
            case "inventory" -> PdfExportUtil.buildInventory(
                    inventoryAnalytics.getAnalytics(from, to), dateRange, generatedBy);
            default -> PdfExportUtil.buildExecutive(
                    executiveDashboardService.getDashboard(), dateRange, generatedBy);
        };
    }

// Default range: last 30 days
    private LocalDate[] resolveRange(LocalDate from, LocalDate to) {
        LocalDate end   = to   != null ? to   : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);
        return new LocalDate[]{start, end};
    }
}
