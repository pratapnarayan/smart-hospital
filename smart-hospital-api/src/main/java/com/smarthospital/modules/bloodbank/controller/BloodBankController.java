package com.smarthospital.modules.bloodbank.controller;

import com.smarthospital.core.pagination.PageResponse;
import com.smarthospital.modules.bloodbank.domain.BloodGroup;
import com.smarthospital.modules.bloodbank.domain.BloodRequest.RequestStatus;
import com.smarthospital.modules.bloodbank.domain.BloodUnit.UnitStatus;
import com.smarthospital.modules.bloodbank.domain.ComponentType;
import com.smarthospital.modules.bloodbank.dto.*;
import com.smarthospital.modules.bloodbank.service.BloodBankService;
import com.smarthospital.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bloodbank")
@Tag(name = "Blood Bank", description = "Donors, blood units, requests and issue management")
public class BloodBankController {

    private final BloodBankService service;

    public BloodBankController(BloodBankService service) {
        this.service = service;
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('BLOODBANK.VIEW')")
    @Operation(summary = "Blood bank dashboard — availability matrix and today's stats")
    public ResponseEntity<ApiResponse<BloodBankDashboardResponse>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(service.getDashboard()));
    }

    // ── Donors ────────────────────────────────────────────────────────────────

    @PostMapping("/donors")
    @PreAuthorize("hasAuthority('BLOODBANK.CREATE')")
    @Operation(summary = "Register a new blood donor")
    public ResponseEntity<ApiResponse<BloodDonorResponse>> registerDonor(
            @Valid @RequestBody BloodDonorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.registerDonor(request)));
    }

    @GetMapping("/donors/{id}")
    @PreAuthorize("hasAuthority('BLOODBANK.VIEW')")
    @Operation(summary = "Get donor details")
    public ResponseEntity<ApiResponse<BloodDonorResponse>> getDonor(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDonor(id)));
    }

    @GetMapping("/donors")
    @PreAuthorize("hasAuthority('BLOODBANK.VIEW')")
    @Operation(summary = "List donors — search by name/mobile or filter by blood group")
    public ResponseEntity<ApiResponse<PageResponse<BloodDonorResponse>>> listDonors(
            @RequestParam(required = false) String     q,
            @RequestParam(required = false) BloodGroup bloodGroup,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(service.listDonors(q, bloodGroup, pageable)));
    }

    // ── Blood Units ───────────────────────────────────────────────────────────

    @PostMapping("/units")
    @PreAuthorize("hasAuthority('BLOODBANK.CREATE')")
    @Operation(summary = "Add a new blood unit (post-collection)")
    public ResponseEntity<ApiResponse<BloodUnitResponse>> addUnit(
            @Valid @RequestBody BloodUnitCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.addUnit(request)));
    }

    @GetMapping("/units/{id}")
    @PreAuthorize("hasAuthority('BLOODBANK.VIEW')")
    @Operation(summary = "Get blood unit details")
    public ResponseEntity<ApiResponse<BloodUnitResponse>> getUnit(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getUnit(id)));
    }

    @GetMapping("/units")
    @PreAuthorize("hasAuthority('BLOODBANK.VIEW')")
    @Operation(summary = "List blood units — filter by blood group, component type, and status")
    public ResponseEntity<ApiResponse<PageResponse<BloodUnitResponse>>> listUnits(
            @RequestParam(required = false) BloodGroup     bloodGroup,
            @RequestParam(required = false) ComponentType  componentType,
            @RequestParam(required = false) UnitStatus     status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("expiryDate").ascending());
        return ResponseEntity.ok(ApiResponse.ok(service.listUnits(bloodGroup, componentType, status, pageable)));
    }

    @PatchMapping("/units/{id}/status")
    @PreAuthorize("hasAuthority('BLOODBANK.EDIT')")
    @Operation(summary = "Update unit status (clear/reject testing, discard, etc.)")
    public ResponseEntity<ApiResponse<BloodUnitResponse>> updateUnitStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUnitStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateUnitStatus(id, request)));
    }

    @GetMapping("/units/available")
    @PreAuthorize("hasAuthority('BLOODBANK.VIEW')")
    @Operation(summary = "Get available units, optionally filtered by blood group and component type (FEFO order)")
    public ResponseEntity<ApiResponse<List<BloodUnitResponse>>> getAvailableUnits(
            @RequestParam(required = false) BloodGroup    bloodGroup,
            @RequestParam(required = false) ComponentType componentType) {
        return ResponseEntity.ok(ApiResponse.ok(service.getAvailableUnits(bloodGroup, componentType)));
    }

    // ── Requests ──────────────────────────────────────────────────────────────

    @PostMapping("/requests")
    @PreAuthorize("hasAuthority('BLOODBANK.CREATE')")
    @Operation(summary = "Create a blood request for a patient")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> createRequest(
            @Valid @RequestBody BloodRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.createRequest(request)));
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('BLOODBANK.VIEW')")
    @Operation(summary = "Get blood request details with issued units")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> getRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getRequest(id)));
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('BLOODBANK.VIEW')")
    @Operation(summary = "List blood requests — filter by status")
    public ResponseEntity<ApiResponse<PageResponse<BloodRequestResponse>>> listRequests(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(service.listRequests(status, pageable)));
    }

    @DeleteMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('BLOODBANK.EDIT')")
    @Operation(summary = "Cancel a blood request")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> cancelRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.cancelRequest(id)));
    }

    @GetMapping("/requests/{id}/issues")
    @PreAuthorize("hasAuthority('BLOODBANK.VIEW')")
    @Operation(summary = "Get all blood issues linked to a request")
    public ResponseEntity<ApiResponse<List<BloodIssueResponse>>> getRequestIssues(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getIssuesForRequest(id)));
    }

    // ── Issues ────────────────────────────────────────────────────────────────

    @PostMapping("/issues")
    @PreAuthorize("hasAuthority('BLOODBANK.EDIT')")
    @Operation(summary = "Issue a blood unit against a request")
    public ResponseEntity<ApiResponse<BloodIssueResponse>> issueBlood(
            @Valid @RequestBody BloodIssueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.issueBlood(request)));
    }

    @GetMapping("/issues")
    @PreAuthorize("hasAuthority('BLOODBANK.VIEW')")
    @Operation(summary = "List all blood issues (most recent first)")
    public ResponseEntity<ApiResponse<PageResponse<BloodIssueResponse>>> listIssues(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(service.listIssues(pageable)));
    }
}
