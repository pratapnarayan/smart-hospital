package com.smarthospital.modules.ipd.service;

import com.smarthospital.core.exception.ApiException;
import com.smarthospital.core.pagination.PageResponse;
import com.smarthospital.modules.ipd.domain.IpdAdmission;
import com.smarthospital.modules.ipd.domain.IpdAdmission.AdmissionStatus;
import com.smarthospital.modules.ipd.domain.IpdBed;
import com.smarthospital.modules.ipd.domain.IpdBed.BedStatus;
import com.smarthospital.modules.ipd.domain.IpdCharge;
import com.smarthospital.modules.ipd.domain.IpdWard;
import com.smarthospital.modules.ipd.dto.*;
import com.smarthospital.modules.ipd.repository.IpdAdmissionRepository;
import com.smarthospital.modules.ipd.repository.IpdBedRepository;
import com.smarthospital.modules.ipd.repository.IpdChargeRepository;
import com.smarthospital.modules.ipd.repository.IpdWardRepository;
import com.smarthospital.modules.patient.domain.Patient;
import com.smarthospital.modules.patient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class IpdService {

    private static final Logger log = LoggerFactory.getLogger(IpdService.class);

    private final IpdAdmissionRepository admissionRepository;
    private final IpdBedRepository       bedRepository;
    private final IpdWardRepository      wardRepository;
    private final PatientRepository      patientRepository;
    private final IpdChargeRepository    chargeRepository;

    public IpdService(IpdAdmissionRepository admissionRepository,
                      IpdBedRepository       bedRepository,
                      IpdWardRepository      wardRepository,
                      PatientRepository      patientRepository,
                      IpdChargeRepository    chargeRepository) {
        this.admissionRepository = admissionRepository;
        this.bedRepository       = bedRepository;
        this.wardRepository      = wardRepository;
        this.patientRepository   = patientRepository;
        this.chargeRepository    = chargeRepository;
    }

    // ── Ward management ──────────────────────────────────────────────────────

    public List<WardResponse> listWards() {
        return wardRepository.findByActiveTrue().stream().map(WardResponse::from).toList();
    }

    @Transactional
    public WardResponse createWard(WardRequest req) {
        if (wardRepository.existsByNameIgnoreCase(req.name())) {
            throw ApiException.conflict("WARD_EXISTS", "Ward '" + req.name() + "' already exists");
        }
        IpdWard ward = IpdWard.builder()
                .name(req.name())
                .wardType(req.wardType())
                .totalBeds(req.totalBeds())
                .active(true)
                .build();
        return WardResponse.from(wardRepository.save(ward));
    }

    // ── Bed management ───────────────────────────────────────────────────────

    public List<BedResponse> listBeds(UUID wardId) {
        wardRepository.findById(wardId)
                .orElseThrow(() -> ApiException.notFound("WARD_NOT_FOUND", "Ward " + wardId + " not found"));
        return bedRepository.findByWardId(wardId).stream().map(BedResponse::from).toList();
    }

    public List<BedResponse> listAvailableBeds(UUID wardId) {
        return bedRepository.findByWardIdAndStatus(wardId, BedStatus.AVAILABLE)
                .stream().map(BedResponse::from).toList();
    }

    @Transactional
    public BedResponse addBed(UUID wardId, BedCreateRequest req) {
        wardRepository.findById(wardId)
                .orElseThrow(() -> ApiException.notFound("WARD_NOT_FOUND", "Ward " + wardId + " not found"));
        if (bedRepository.existsByWardIdAndBedNumberIgnoreCase(wardId, req.bedNumber())) {
            throw ApiException.conflict("BED_EXISTS",
                    "Bed '" + req.bedNumber() + "' already exists in this ward");
        }
        IpdBed bed = IpdBed.builder()
                .wardId(wardId)
                .bedNumber(req.bedNumber())
                .bedType(req.bedType())
                .dailyCharge(req.dailyCharge() != null ? req.dailyCharge() : java.math.BigDecimal.ZERO)
                .build();
        return BedResponse.from(bedRepository.save(bed));
    }

    @Transactional
    public BedResponse updateBedStatus(UUID bedId, BedStatus status) {
        IpdBed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> ApiException.notFound("BED_NOT_FOUND", "Bed " + bedId + " not found"));
        if (status == BedStatus.AVAILABLE
                && admissionRepository.existsByBedIdAndStatus(bedId, AdmissionStatus.ADMITTED)) {
            throw ApiException.badRequest("BED_OCCUPIED", "Cannot free a bed with an active admission");
        }
        bed.setStatus(status);
        return BedResponse.from(bedRepository.save(bed));
    }

    // ── Admissions ───────────────────────────────────────────────────────────

    @Transactional
    public IpdAdmissionResponse admitPatient(IpdAdmissionCreateRequest req) {
        Patient patient = patientRepository.findById(req.patientId())
                .orElseThrow(() -> ApiException.notFound("PATIENT_NOT_FOUND",
                        "Patient " + req.patientId() + " not found"));

        wardRepository.findById(req.wardId())
                .orElseThrow(() -> ApiException.notFound("WARD_NOT_FOUND", "Ward " + req.wardId() + " not found"));

        IpdBed bed = bedRepository.findById(req.bedId())
                .orElseThrow(() -> ApiException.notFound("BED_NOT_FOUND", "Bed " + req.bedId() + " not found"));

        if (bed.getStatus() != BedStatus.AVAILABLE) {
            throw ApiException.conflict("BED_NOT_AVAILABLE",
                    "Bed " + bed.getBedNumber() + " is not available (status: " + bed.getStatus() + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        IpdAdmission admission = IpdAdmission.builder()
                .admissionNumber(generateAdmissionNumber(now))
                .patientId(patient.getId())
                .patientName(patient.getFirstName() + " " + patient.getLastName())
                .opdVisitId(req.opdVisitId())
                .admissionDate(now)
                .wardId(req.wardId())
                .bedId(req.bedId())
                .doctorId(req.doctorId())
                .doctorName(req.doctorName())
                .admissionDiagnosis(req.admissionDiagnosis())
                .build();

        bed.setStatus(BedStatus.OCCUPIED);
        bedRepository.save(bed);

        IpdAdmission saved = admissionRepository.save(admission);
        log.info("Patient {} admitted as {}", patient.getId(), saved.getAdmissionNumber());
        return IpdAdmissionResponse.from(saved);
    }

    public IpdAdmissionResponse getAdmission(UUID id) {
        return IpdAdmissionResponse.from(findOrThrow(id));
    }

    public PageResponse<IpdAdmissionResponse> listAdmissions(Pageable pageable) {
        return PageResponse.of(admissionRepository.findAll(pageable).map(IpdAdmissionResponse::from));
    }

    public PageResponse<IpdAdmissionResponse> listByStatus(AdmissionStatus status, Pageable pageable) {
        return PageResponse.of(admissionRepository.findByStatus(status, pageable)
                .map(IpdAdmissionResponse::from));
    }

    public PageResponse<IpdAdmissionResponse> listByPatient(UUID patientId, Pageable pageable) {
        return PageResponse.of(admissionRepository.findByPatientId(patientId, pageable)
                .map(IpdAdmissionResponse::from));
    }

    @Transactional
    public IpdAdmissionResponse updateAdmission(UUID id, IpdAdmissionUpdateRequest req) {
        IpdAdmission admission = findOrThrow(id);

        if (req.doctorId()          != null) admission.setDoctorId(req.doctorId());
        if (req.doctorName()        != null) admission.setDoctorName(req.doctorName());
        if (req.admissionDiagnosis() != null) admission.setAdmissionDiagnosis(req.admissionDiagnosis());
        if (req.notes()             != null) admission.setNotes(req.notes());
        if (req.discount()          != null) admission.setDiscount(req.discount());
        if (req.paymentStatus()     != null) admission.setPaymentStatus(req.paymentStatus());

        if (req.bedId() != null && !req.bedId().equals(admission.getBedId())) {
            IpdBed newBed = bedRepository.findById(req.bedId())
                    .orElseThrow(() -> ApiException.notFound("BED_NOT_FOUND", "Bed not found"));
            if (newBed.getStatus() != BedStatus.AVAILABLE) {
                throw ApiException.conflict("BED_NOT_AVAILABLE", "Target bed is not available");
            }
            // Free old bed, occupy new one
            bedRepository.findById(admission.getBedId()).ifPresent(old -> {
                old.setStatus(BedStatus.AVAILABLE);
                bedRepository.save(old);
            });
            newBed.setStatus(BedStatus.OCCUPIED);
            bedRepository.save(newBed);
            admission.setBedId(req.bedId());
            if (req.wardId() != null) admission.setWardId(req.wardId());
        }

        admission.recalculateTotals();
        return IpdAdmissionResponse.from(admissionRepository.save(admission));
    }

    @Transactional
    public IpdAdmissionResponse dischargePatient(UUID id, IpdDischargeRequest req) {
        IpdAdmission admission = findOrThrow(id);
        if (admission.getStatus() != AdmissionStatus.ADMITTED) {
            throw ApiException.badRequest("INVALID_STATUS",
                    "Only ADMITTED patients can be discharged (current: " + admission.getStatus() + ")");
        }

        admission.setStatus(AdmissionStatus.DISCHARGED);
        admission.setDischargeDate(LocalDateTime.now());
        admission.setConditionAtDischarge(req.conditionAtDischarge());
        admission.setFinalDiagnosis(req.finalDiagnosis());
        admission.setDischargeNotes(req.dischargeNotes());
        admission.setFollowUpInstructions(req.followUpInstructions());

        // Free the bed
        bedRepository.findById(admission.getBedId()).ifPresent(bed -> {
            bed.setStatus(BedStatus.AVAILABLE);
            bedRepository.save(bed);
        });

        admission.recalculateTotals();
        IpdAdmission saved = admissionRepository.save(admission);
        log.info("Patient {} discharged from admission {}", admission.getPatientId(), saved.getAdmissionNumber());
        return IpdAdmissionResponse.from(saved);
    }

    // ── Charges ──────────────────────────────────────────────────────────────

    @Transactional
    public IpdAdmissionResponse addCharge(UUID admissionId, IpdChargeRequest req) {
        IpdAdmission admission = findOrThrow(admissionId);
        if (admission.getStatus() == AdmissionStatus.DISCHARGED) {
            throw ApiException.badRequest("ADMISSION_CLOSED", "Cannot add charges to a discharged patient");
        }

        LocalDate chargeDate = req.chargeDate() != null ? req.chargeDate() : LocalDate.now();
        IpdCharge charge = new IpdCharge(admission, req.category(),
                req.description(), req.amount(), chargeDate);
        chargeRepository.save(charge);  // persist explicitly — em.merge() on admission doesn't cascade-persist new transients
        admission.getCharges().add(charge);
        admission.recalculateTotals();
        return IpdAdmissionResponse.from(admissionRepository.save(admission));
    }

    // ── Dashboard ────────────────────────────────────────────────────────────

    public IpdDashboardResponse getDashboard() {
        List<Object[]> statusCounts = admissionRepository.countByStatus();
        long admitted   = extract(statusCounts, AdmissionStatus.ADMITTED.name());
        long discharged = extract(statusCounts, AdmissionStatus.DISCHARGED.name());

        long totalBeds     = bedRepository.count();
        long occupiedBeds  = bedRepository.findAll().stream()
                .filter(b -> b.getStatus() == BedStatus.OCCUPIED).count();
        long availableBeds = bedRepository.findAll().stream()
                .filter(b -> b.getStatus() == BedStatus.AVAILABLE).count();

        return new IpdDashboardResponse(admitted, discharged, totalBeds, availableBeds, occupiedBeds);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private IpdAdmission findOrThrow(UUID id) {
        return admissionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ADMISSION_NOT_FOUND",
                        "IPD admission " + id + " not found"));
    }

    private String generateAdmissionNumber(LocalDateTime date) {
        long seq = admissionRepository.nextSequenceForYear(date.getYear());
        return String.format("IPD-%d-%05d", date.getYear(), seq);
    }

    private long extract(List<Object[]> rows, String statusName) {
        return rows.stream()
                .filter(r -> r[0].toString().equals(statusName))
                .mapToLong(r -> ((Number) r[1]).longValue())
                .findFirst().orElse(0L);
    }
}
