package com.smarthospital.modules.clinic.service;

import com.smarthospital.core.exception.ApiException;
import com.smarthospital.modules.clinic.domain.ClinicVisitBill;
import com.smarthospital.modules.clinic.domain.ClinicVisitBillItem;
import com.smarthospital.modules.clinic.dto.ClinicBillCreateRequest;
import com.smarthospital.modules.clinic.dto.ClinicBillResponse;
import com.smarthospital.modules.clinic.repository.ClinicLabQueryRepository;
import com.smarthospital.modules.clinic.repository.ClinicVisitBillRepository;
import com.smarthospital.modules.opd.domain.OpdVisit;
import com.smarthospital.modules.opd.repository.OpdVisitRepository;
import com.smarthospital.modules.pathology.domain.LabOrder;
import com.smarthospital.modules.pharmacy.domain.PharmacyBill;
import com.smarthospital.modules.pharmacy.repository.PharmacyBillRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class ClinicBillService {

    private final ClinicVisitBillRepository billRepository;
    private final OpdVisitRepository opdVisitRepository;
    private final ClinicLabQueryRepository labQueryRepository;
    private final PharmacyBillRepository pharmacyBillRepository;

    public ClinicBillService(ClinicVisitBillRepository billRepository,
                             OpdVisitRepository opdVisitRepository,
                             ClinicLabQueryRepository labQueryRepository,
                             PharmacyBillRepository pharmacyBillRepository) {
        this.billRepository = billRepository;
        this.opdVisitRepository = opdVisitRepository;
        this.labQueryRepository = labQueryRepository;
        this.pharmacyBillRepository = pharmacyBillRepository;
    }

    @Transactional
    public ClinicBillResponse generateBill(ClinicBillCreateRequest req) {
        OpdVisit visit = opdVisitRepository.findById(req.opdVisitId())
                .orElseThrow(() -> ApiException.notFound("OPD_VISIT_NOT_FOUND",
                        "OPD visit not found: " + req.opdVisitId()));

        ClinicVisitBill bill = new ClinicVisitBill();
        bill.setOpdVisitId(visit.getId());
        bill.setPatientId(visit.getPatientId());
        bill.setPatientName(visit.getPatientName());
        bill.setVisitDate(visit.getVisitDate());
        bill.setStatus("DRAFT");

        int seq = billRepository.nextDailySequence(visit.getVisitDate());
        String billNumber = "CB-"
                + visit.getVisitDate().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%04d", seq);
        bill.setBillNumber(billNumber);

        BigDecimal total = BigDecimal.ZERO;

        // CONSULTATION: consultation fee (BigDecimal, defaults to ZERO)
        BigDecimal consultFee = visit.getConsultationFee();
        if (consultFee != null && consultFee.compareTo(BigDecimal.ZERO) > 0) {
            total = total.add(addItem(bill, "CONSULTATION", "Consultation Fee", consultFee, visit.getId()));
        }

        // CONSULTATION: additional OPD charge lines
        if (visit.getCharges() != null) {
            for (var charge : visit.getCharges()) {
                BigDecimal amt = charge.getAmount();
                if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
                    total = total.add(addItem(bill, "CONSULTATION",
                            charge.getDescription(), amt, visit.getId()));
                }
            }
        }

        // PATHOLOGY: lab orders linked to this OPD visit via sourceId
        List<LabOrder> labOrders = labQueryRepository.findBySourceId(visit.getId());
        for (LabOrder order : labOrders) {
            BigDecimal amt = order.getNetAmount();
            if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(addItem(bill, "PATHOLOGY",
                        "Lab Order: " + order.getOrderNumber(), amt, order.getId()));
            }
        }

        // PHARMACY: bills for the same patient created on the same calendar date as the visit
        // PharmacyBill.getCreatedAt() is Instant (from CreatedOnlyAuditEntity)
        var pharmacyPage = pharmacyBillRepository.findByPatientId(
                visit.getPatientId(), PageRequest.of(0, 100));
        for (PharmacyBill pb : pharmacyPage.getContent()) {
            if (pb.getCreatedAt() != null
                    && pb.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate()
                            .equals(visit.getVisitDate())) {
                BigDecimal amt = pb.getNetAmount();
                if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
                    total = total.add(addItem(bill, "PHARMACY",
                            "Pharmacy Bill: " + pb.getBillNumber(), amt, pb.getId()));
                }
            }
        }

        bill.setTotalAmount(total);
        return ClinicBillResponse.from(billRepository.save(bill));
    }

    @Transactional(readOnly = true)
    public List<ClinicBillResponse> findByOpdVisit(UUID opdVisitId) {
        return billRepository.findByOpdVisitId(opdVisitId)
                .stream().map(ClinicBillResponse::from).toList();
    }

    @Transactional
    public ClinicBillResponse finalize(UUID id) {
        ClinicVisitBill bill = getOrThrow(id);
        if (!"DRAFT".equals(bill.getStatus())) {
            throw ApiException.badRequest("INVALID_STATE", "Only DRAFT bills can be finalized");
        }
        if (bill.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("EMPTY_BILL", "Cannot finalize a bill with zero total");
        }
        bill.setStatus("FINALIZED");
        return ClinicBillResponse.from(billRepository.save(bill));
    }

    @Transactional
    public ClinicBillResponse cancel(UUID id) {
        ClinicVisitBill bill = getOrThrow(id);
        if ("FINALIZED".equals(bill.getStatus())) {
            throw ApiException.badRequest("INVALID_STATE", "Finalized bills cannot be cancelled");
        }
        bill.setStatus("CANCELLED");
        return ClinicBillResponse.from(billRepository.save(bill));
    }

    @Transactional(readOnly = true)
    public List<ClinicBillResponse> findByPatient(UUID patientId) {
        return billRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream().map(ClinicBillResponse::from).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ClinicVisitBill getOrThrow(UUID id) {
        return billRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("BILL_NOT_FOUND",
                        "Clinic bill not found: " + id));
    }

    private BigDecimal addItem(ClinicVisitBill bill, String lineType,
                               String description, BigDecimal amount, UUID sourceId) {
        ClinicVisitBillItem item = new ClinicVisitBillItem();
        item.setBill(bill);
        item.setLineType(lineType);
        item.setDescription(description);
        item.setAmount(amount);
        item.setSourceId(sourceId);
        bill.getItems().add(item);
        return amount;
    }
}
