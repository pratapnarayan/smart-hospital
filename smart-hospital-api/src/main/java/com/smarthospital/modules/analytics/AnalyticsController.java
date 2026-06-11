package com.smarthospital.modules.analytics;

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

    // Default range: last 30 days
    private LocalDate[] resolveRange(LocalDate from, LocalDate to) {
        LocalDate end   = to   != null ? to   : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);
        return new LocalDate[]{start, end};
    }
}
