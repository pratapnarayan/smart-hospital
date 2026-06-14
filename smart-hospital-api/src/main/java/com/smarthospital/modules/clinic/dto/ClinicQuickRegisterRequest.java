package com.smarthospital.modules.clinic.dto;

import jakarta.validation.constraints.*;

public record ClinicQuickRegisterRequest(
        @NotBlank String name,
        @Pattern(regexp = "\\d{10}", message = "Phone must be 10 digits") String phone,
        @Min(0) @Max(150) Integer age,
        String gender
) {}
