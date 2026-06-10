package com.smarthospital.modules.doctor.dto;

import com.smarthospital.modules.doctor.domain.Specialization;

import java.util.UUID;

public record SpecializationResponse(UUID id, String name, String code, String description, boolean active) {
    public static SpecializationResponse from(Specialization s) {
        return new SpecializationResponse(s.getId(), s.getName(), s.getCode(), s.getDescription(), s.isActive());
    }
}
