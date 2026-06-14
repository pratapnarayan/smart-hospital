package com.smarthospital.modules.clinic.dto;

import com.smarthospital.modules.clinic.domain.ClinicVisitBillItem;
import java.math.BigDecimal;
import java.util.UUID;

public record ClinicBillItemResponse(
        UUID id,
        String lineType,
        String description,
        BigDecimal amount,
        UUID sourceId
) {
    public static ClinicBillItemResponse from(ClinicVisitBillItem i) {
        return new ClinicBillItemResponse(
                i.getId(),
                i.getLineType(),
                i.getDescription(),
                i.getAmount(),
                i.getSourceId()
        );
    }
}
