package com.smarthospital.modules.clinic.dto;

import com.smarthospital.modules.clinic.domain.CollectionStatus;
import com.smarthospital.modules.clinic.domain.HomeCollection;

import java.time.Instant;
import java.util.UUID;

public record HomeCollectionResponse(
        UUID id,
        UUID patientId,
        String patientName,
        String patientPhone,
        String address,
        Instant scheduledAt,
        Instant collectedAt,
        UUID technicianId,
        String technicianName,
        CollectionStatus status,
        String failureReason,
        String notes,
        Instant createdAt
) {
    public static HomeCollectionResponse from(HomeCollection h) {
        return new HomeCollectionResponse(
                h.getId(), h.getPatientId(), h.getPatientName(), h.getPatientPhone(),
                h.getAddress(), h.getScheduledAt(), h.getCollectedAt(),
                h.getTechnicianId(), h.getTechnicianName(), h.getStatus(),
                h.getFailureReason(), h.getNotes(), h.getCreatedAt()
        );
    }
}
