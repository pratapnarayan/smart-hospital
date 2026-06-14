package com.smarthospital.modules.clinic.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ClinicBillCreateRequest(
        @NotNull UUID opdVisitId
) {}
