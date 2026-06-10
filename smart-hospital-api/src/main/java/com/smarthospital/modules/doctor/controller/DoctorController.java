package com.smarthospital.modules.doctor.controller;

import com.smarthospital.core.exception.ApiException;
import com.smarthospital.core.pagination.PageResponse;
import com.smarthospital.modules.doctor.dto.*;
import com.smarthospital.modules.doctor.service.DoctorService;
import com.smarthospital.shared.dto.ApiResponse;
import com.smarthospital.shared.service.FileStorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doctor")
@Tag(name = "Doctors", description = "Doctor profiles, schedules and availability")
public class DoctorController {

    private final DoctorService      service;
    private final FileStorageService fileStorage;

    public DoctorController(DoctorService service, FileStorageService fileStorage) {
        this.service     = service;
        this.fileStorage = fileStorage;
    }

    // ── Specializations ──────────────────────────────────────────────────────

    @GetMapping("/specializations")
    @PreAuthorize("hasAuthority('DOCTOR.VIEW')")
    public ResponseEntity<ApiResponse<List<SpecializationResponse>>> listSpecializations() {
        return ResponseEntity.ok(ApiResponse.ok(service.listSpecializations()));
    }

    @PostMapping("/specializations")
    @PreAuthorize("hasAuthority('DOCTOR.MANAGE')")
    public ResponseEntity<ApiResponse<SpecializationResponse>> createSpecialization(
            @Valid @RequestBody SpecializationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.createSpecialization(req)));
    }

    @PatchMapping("/specializations/{id}")
    @PreAuthorize("hasAuthority('DOCTOR.MANAGE')")
    public ResponseEntity<ApiResponse<SpecializationResponse>> updateSpecialization(
            @PathVariable UUID id, @Valid @RequestBody SpecializationRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateSpecialization(id, req)));
    }

    // ── Doctor Profiles ──────────────────────────────────────────────────────

    @GetMapping("/doctors")
    @PreAuthorize("hasAuthority('DOCTOR.VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<DoctorProfileResponse>>> listDoctors(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID specializationId,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size) {
        int clampedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(page, clampedSize, Sort.by("id").ascending());
        return ResponseEntity.ok(ApiResponse.ok(service.listDoctors(search, departmentId, specializationId, pageable)));
    }

    @GetMapping("/doctors/{id}")
    @PreAuthorize("hasAuthority('DOCTOR.VIEW')")
    public ResponseEntity<ApiResponse<DoctorProfileResponse>> getDoctor(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDoctor(id)));
    }

    @GetMapping("/doctors/by-employee/{employeeId}")
    @PreAuthorize("hasAuthority('DOCTOR.VIEW')")
    public ResponseEntity<ApiResponse<DoctorProfileResponse>> getDoctorByEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDoctorByEmployeeId(employeeId)));
    }

    @PostMapping("/doctors")
    @PreAuthorize("hasAuthority('DOCTOR.CREATE')")
    public ResponseEntity<ApiResponse<DoctorProfileResponse>> createDoctor(@RequestBody DoctorProfileRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.createDoctorProfile(req)));
    }

    @PatchMapping("/doctors/{id}")
    @PreAuthorize("hasAuthority('DOCTOR.EDIT')")
    public ResponseEntity<ApiResponse<DoctorProfileResponse>> updateDoctor(
            @PathVariable UUID id, @RequestBody DoctorProfileRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateDoctorProfile(id, req)));
    }

    @PostMapping(value = "/doctors/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DOCTOR.EDIT')")
    public ResponseEntity<ApiResponse<DoctorProfileResponse>> uploadDoctorPhoto(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        service.getDoctor(id);  // verify exists → 404 before writing to storage
        String url = fileStorage.store(file, "doc_" + id);
        try {
            return ResponseEntity.ok(ApiResponse.ok(service.updateDoctorPhoto(id, url)));
        } catch (Exception e) {
            fileStorage.delete(url);
            throw e;
        }
    }

    // ── Schedules ────────────────────────────────────────────────────────────

    @GetMapping("/doctors/{id}/schedules")
    @PreAuthorize("hasAuthority('DOCTOR.VIEW')")
    public ResponseEntity<ApiResponse<List<DoctorScheduleResponse>>> getSchedules(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getSchedules(id)));
    }

    @PutMapping("/doctors/{id}/schedules")
    @PreAuthorize("hasAuthority('DOCTOR.EDIT')")
    public ResponseEntity<ApiResponse<List<DoctorScheduleResponse>>> saveSchedules(
            @PathVariable UUID id, @RequestBody List<DoctorScheduleRequest> requests) {
        return ResponseEntity.ok(ApiResponse.ok(service.saveSchedules(id, requests)));
    }

    // ── Availability ─────────────────────────────────────────────────────────

    @GetMapping("/doctors/{id}/availability")
    @PreAuthorize("hasAuthority('DOCTOR.VIEW')")
    public ResponseEntity<ApiResponse<List<AvailableSlotResponse>>> getAvailability(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end   = to   != null ? to   : start.plusDays(30);
        if (end.isBefore(start)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE", "'to' date must not be before 'from' date");
        }
        return ResponseEntity.ok(ApiResponse.ok(service.getAvailableSlots(id, start, end)));
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('DOCTOR.VIEW')")
    public ResponseEntity<ApiResponse<DoctorDashboardResponse>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(service.getDashboard()));
    }
}
