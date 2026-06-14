package com.smarthospital.modules.clinic.dto;

import com.smarthospital.modules.clinic.domain.CollectionStatus;
import jakarta.validation.constraints.NotNull;

public record HomeCollectionStatusRequest(
        @NotNull CollectionStatus status,
        String failureReason
) {}
