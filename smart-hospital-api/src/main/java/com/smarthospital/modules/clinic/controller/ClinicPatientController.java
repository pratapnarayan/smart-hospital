package com.smarthospital.modules.clinic.controller;

import com.smarthospital.modules.clinic.dto.ClinicQuickRegisterRequest;
import com.smarthospital.modules.clinic.service.ClinicPatientService;
import com.smarthospital.modules.patient.dto.PatientResponse;
import com.smarthospital.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clinic/patients")
@Tag(name = "Clinic - Patients")
public class ClinicPatientController {

    private final ClinicPatientService service;

    public ClinicPatientController(ClinicPatientService service) {
        this.service = service;
    }

    @PostMapping("/quick-register")
    @PreAuthorize("hasAuthority('PATIENT.CREATE')")
    @Operation(summary = "Quick register a patient for a clinic visit")
    public ResponseEntity<ApiResponse<PatientResponse>> quickRegister(
            @Valid @RequestBody ClinicQuickRegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.quickRegister(req)));
    }
}
