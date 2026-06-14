package com.smarthospital.modules.clinic.dto;

import com.smarthospital.modules.clinic.domain.ClinicVisitBill;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClinicBillResponse(
        UUID id,
        String billNumber,
        UUID opdVisitId,
        UUID patientId,
        String patientName,
        LocalDate visitDate,
        BigDecimal totalAmount,
        String status,
        List<ClinicBillItemResponse> items,
        Instant createdAt
) {
    public static ClinicBillResponse from(ClinicVisitBill b) {
        return new ClinicBillResponse(
                b.getId(),
                b.getBillNumber(),
                b.getOpdVisitId(),
                b.getPatientId(),
                b.getPatientName(),
                b.getVisitDate(),
                b.getTotalAmount(),
                b.getStatus(),
                b.getItems().stream().map(ClinicBillItemResponse::from).toList(),
                b.getCreatedAt()
        );
    }
}
