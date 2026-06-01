package com.smarthospital.modules.frontoffice.controller;

import com.smarthospital.core.pagination.PageResponse;
import com.smarthospital.modules.frontoffice.domain.OpdToken.TokenStatus;
import com.smarthospital.modules.frontoffice.dto.*;
import com.smarthospital.modules.frontoffice.service.FrontOfficeService;
import com.smarthospital.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/frontoffice")
@Tag(name = "Front Office", description = "Appointments and OPD token queue")
public class FrontOfficeController {

    private final FrontOfficeService service;

    public FrontOfficeController(FrontOfficeService service) {
        this.service = service;
    }

    // ── Appointments ─────────────────────────────────────────────────────────

    @PostMapping("/appointments")
    @PreAuthorize("hasAuthority('FRONTOFFICE.CREATE')")
    @Operation(summary = "Book a new appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody AppointmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.createAppointment(request)));
    }

    @GetMapping("/appointments/{id}")
    @PreAuthorize("hasAuthority('FRONTOFFICE.VIEW')")
    @Operation(summary = "Get appointment details")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getAppointment(id)));
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasAuthority('FRONTOFFICE.VIEW')")
    @Operation(summary = "List appointments for a date (defaults to today)")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> listByDate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timeSlot").ascending());
        return ResponseEntity.ok(ApiResponse.ok(service.listByDate(date, pageable)));
    }

    @GetMapping("/appointments/upcoming")
    @PreAuthorize("hasAuthority('FRONTOFFICE.VIEW')")
    @Operation(summary = "List all upcoming appointments (today onwards, SCHEDULED or CONFIRMED)")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> listUpcoming(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "100") int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("appointmentDate").ascending().and(Sort.by("timeSlot").ascending()));
        return ResponseEntity.ok(ApiResponse.ok(service.listUpcoming(pageable)));
    }

    @GetMapping("/appointments/patient/{patientId}")
    @PreAuthorize("hasAuthority('FRONTOFFICE.VIEW')")
    @Operation(summary = "List all appointments for a patient")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> listByPatient(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("appointmentDate").descending());
        return ResponseEntity.ok(ApiResponse.ok(service.listByPatient(patientId, pageable)));
    }

    @GetMapping("/appointments/patient/{patientId}/upcoming")
    @PreAuthorize("hasAuthority('FRONTOFFICE.VIEW')")
    @Operation(summary = "List upcoming appointments for a patient (today onwards, active statuses)")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> listUpcomingByPatient(
            @PathVariable UUID patientId) {
        return ResponseEntity.ok(ApiResponse.ok(service.listUpcomingByPatient(patientId)));
    }

    @GetMapping("/appointments/doctor/{doctorId}")
    @PreAuthorize("hasAuthority('FRONTOFFICE.VIEW')")
    @Operation(summary = "List appointments for a doctor on a given date")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> listByDoctor(
            @PathVariable UUID doctorId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timeSlot").ascending());
        return ResponseEntity.ok(ApiResponse.ok(service.listByDoctor(doctorId, date, pageable)));
    }

    @PatchMapping("/appointments/{id}")
    @PreAuthorize("hasAuthority('FRONTOFFICE.EDIT')")
    @Operation(summary = "Update appointment (reschedule, confirm, check-in, complete)")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateAppointment(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateAppointment(id, request)));
    }

    @DeleteMapping("/appointments/{id}")
    @PreAuthorize("hasAuthority('FRONTOFFICE.EDIT')")
    @Operation(summary = "Cancel an appointment")
    public ResponseEntity<ApiResponse<Void>> cancelAppointment(@PathVariable UUID id) {
        service.cancelAppointment(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── OPD Tokens ────────────────────────────────────────────────────────────

    @PostMapping("/tokens")
    @PreAuthorize("hasAuthority('FRONTOFFICE.CREATE')")
    @Operation(summary = "Issue a walk-in OPD token")
    public ResponseEntity<ApiResponse<OpdTokenResponse>> generateToken(
            @Valid @RequestBody OpdTokenCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.generateToken(request)));
    }

    @GetMapping("/tokens")
    @PreAuthorize("hasAuthority('FRONTOFFICE.VIEW')")
    @Operation(summary = "List today's tokens, optionally filter by department")
    public ResponseEntity<ApiResponse<List<OpdTokenResponse>>> listTokens(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String department) {
        return ResponseEntity.ok(ApiResponse.ok(service.listTokens(date, department)));
    }

    @GetMapping("/tokens/{id}")
    @PreAuthorize("hasAuthority('FRONTOFFICE.VIEW')")
    @Operation(summary = "Get token details")
    public ResponseEntity<ApiResponse<OpdTokenResponse>> getToken(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getToken(id)));
    }

    @PatchMapping("/tokens/{id}/status")
    @PreAuthorize("hasAuthority('FRONTOFFICE.EDIT')")
    @Operation(summary = "Update token status (WAITING → IN_PROGRESS → COMPLETED / SKIPPED)")
    public ResponseEntity<ApiResponse<OpdTokenResponse>> updateTokenStatus(
            @PathVariable UUID id,
            @RequestParam TokenStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateTokenStatus(id, status)));
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('FRONTOFFICE.VIEW')")
    @Operation(summary = "Front office dashboard — today's appointments and queue stats")
    public ResponseEntity<ApiResponse<FrontOfficeDashboardResponse>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(service.getDashboard()));
    }
}
