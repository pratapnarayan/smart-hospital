package com.smarthospital.modules.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record HomeCollectionCreateRequest(
        @NotNull UUID patientId,
        @NotBlank String patientName,
        String patientPhone,
        @NotBlank String address,
        @NotNull Instant scheduledAt,
        UUID technicianId,
        String technicianName,
        String notes
) {}
