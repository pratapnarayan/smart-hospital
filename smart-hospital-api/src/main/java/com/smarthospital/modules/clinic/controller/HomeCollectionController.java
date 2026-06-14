package com.smarthospital.modules.clinic.controller;

import com.smarthospital.modules.clinic.dto.*;
import com.smarthospital.modules.clinic.service.HomeCollectionService;
import com.smarthospital.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinic/home-collections")
@Tag(name = "Clinic - Home Collections")
public class HomeCollectionController {

    private final HomeCollectionService service;

    public HomeCollectionController(HomeCollectionService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLINIC.HOME_COLLECTION.CREATE')")
    @Operation(summary = "Schedule a new home collection")
    public ResponseEntity<ApiResponse<HomeCollectionResponse>> create(
            @Valid @RequestBody HomeCollectionCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(req)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLINIC.HOME_COLLECTION.VIEW')")
    @Operation(summary = "List home collections for a date")
    public ResponseEntity<ApiResponse<HomeCollectionSummaryResponse>> findByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(service.findByDate(date)));
    }

    @GetMapping("/my-schedule")
    @PreAuthorize("hasAuthority('CLINIC.HOME_COLLECTION.VIEW')")
    @Operation(summary = "Get technician's schedule for a date")
    public ResponseEntity<ApiResponse<HomeCollectionSummaryResponse>> findMySchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam UUID technicianId) {
        return ResponseEntity.ok(ApiResponse.ok(service.findByTechnicianAndDate(technicianId, date)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLINIC.HOME_COLLECTION.VIEW')")
    @Operation(summary = "Get home collection by ID")
    public ResponseEntity<ApiResponse<HomeCollectionResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
    }

    @PatchMapping("/{id}/reschedule")
    @PreAuthorize("hasAuthority('CLINIC.HOME_COLLECTION.EDIT')")
    @Operation(summary = "Reschedule a home collection")
    public ResponseEntity<ApiResponse<HomeCollectionResponse>> reschedule(
            @PathVariable UUID id,
            @Valid @RequestBody HomeCollectionUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.reschedule(id, req)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CLINIC.HOME_COLLECTION.EDIT')")
    @Operation(summary = "Update collection status")
    public ResponseEntity<ApiResponse<HomeCollectionResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody HomeCollectionStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateStatus(id, req)));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('CLINIC.HOME_COLLECTION.VIEW')")
    @Operation(summary = "Get collections for a patient")
    public ResponseEntity<ApiResponse<List<HomeCollectionResponse>>> findByPatient(
            @PathVariable UUID patientId) {
        return ResponseEntity.ok(ApiResponse.ok(service.findByPatient(patientId)));
    }
}
