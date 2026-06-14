package com.smarthospital.modules.clinic.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record HomeCollectionUpdateRequest(
        @NotNull Instant scheduledAt,
        UUID technicianId,
        String technicianName,
        String notes
) {}
