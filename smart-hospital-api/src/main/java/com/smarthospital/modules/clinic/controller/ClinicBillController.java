package com.smarthospital.modules.clinic.controller;

import com.smarthospital.modules.clinic.dto.ClinicBillCreateRequest;
import com.smarthospital.modules.clinic.dto.ClinicBillResponse;
import com.smarthospital.modules.clinic.service.ClinicBillService;
import com.smarthospital.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinic/bills")
@Tag(name = "Clinic - Bills")
public class ClinicBillController {

    private final ClinicBillService service;

    public ClinicBillController(ClinicBillService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLINIC.BILL.CREATE')")
    @Operation(summary = "Generate a consolidated visit bill")
    public ResponseEntity<ApiResponse<ClinicBillResponse>> generate(
            @Valid @RequestBody ClinicBillCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.generateBill(req)));
    }

    @GetMapping("/visit/{opdVisitId}")
    @PreAuthorize("hasAuthority('CLINIC.BILL.VIEW')")
    @Operation(summary = "Get bills for an OPD visit")
    public ResponseEntity<ApiResponse<List<ClinicBillResponse>>> findByVisit(
            @PathVariable UUID opdVisitId) {
        return ResponseEntity.ok(ApiResponse.ok(service.findByOpdVisit(opdVisitId)));
    }

    @PatchMapping("/{id}/finalize")
    @PreAuthorize("hasAuthority('CLINIC.BILL.MANAGE')")
    @Operation(summary = "Finalize a draft bill")
    public ResponseEntity<ApiResponse<ClinicBillResponse>> finalize(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.finalize(id)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CLINIC.BILL.MANAGE')")
    @Operation(summary = "Cancel a bill")
    public ResponseEntity<ApiResponse<ClinicBillResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.cancel(id)));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('CLINIC.BILL.VIEW')")
    @Operation(summary = "Get bill history for a patient")
    public ResponseEntity<ApiResponse<List<ClinicBillResponse>>> findByPatient(
            @PathVariable UUID patientId) {
        return ResponseEntity.ok(ApiResponse.ok(service.findByPatient(patientId)));
    }
}
