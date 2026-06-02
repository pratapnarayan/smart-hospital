package com.smarthospital.modules.bloodbank.service;

import com.smarthospital.core.exception.ApiException;
import com.smarthospital.core.pagination.PageResponse;
import com.smarthospital.modules.bloodbank.domain.*;
import com.smarthospital.modules.bloodbank.domain.BloodRequest.RequestStatus;
import com.smarthospital.modules.bloodbank.domain.BloodUnit.TestingStatus;
import com.smarthospital.modules.bloodbank.domain.BloodUnit.UnitStatus;
import com.smarthospital.modules.bloodbank.dto.*;
import com.smarthospital.modules.bloodbank.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class BloodBankService {

    private static final Logger log = LoggerFactory.getLogger(BloodBankService.class);

    private final BloodDonorRepository   donorRepository;
    private final BloodUnitRepository    unitRepository;
    private final BloodRequestRepository requestRepository;
    private final BloodIssueRepository   issueRepository;

    public BloodBankService(BloodDonorRepository   donorRepository,
                            BloodUnitRepository    unitRepository,
                            BloodRequestRepository requestRepository,
                            BloodIssueRepository   issueRepository) {
        this.donorRepository   = donorRepository;
        this.unitRepository    = unitRepository;
        this.requestRepository = requestRepository;
        this.issueRepository   = issueRepository;
    }

    // ── Donors ────────────────────────────────────────────────────────────────

    @Transactional
    public BloodDonorResponse registerDonor(BloodDonorRequest req) {
        BloodDonor donor = BloodDonor.builder()
                .donorNumber(generateDonorNumber())
                .firstName(req.firstName())
                .lastName(req.lastName())
                .gender(req.gender())
                .dateOfBirth(req.dateOfBirth())
                .bloodGroup(req.bloodGroup())
                .mobile(req.mobile())
                .email(req.email())
                .address(req.address())
                .build();
        BloodDonor saved = donorRepository.save(donor);
        log.info("Blood donor {} registered ({} — {})", saved.getDonorNumber(),
                saved.getFirstName() + " " + saved.getLastName(), saved.getBloodGroup().display());
        return BloodDonorResponse.from(saved);
    }

    public BloodDonorResponse getDonor(UUID id) {
        return BloodDonorResponse.from(findDonorOrThrow(id));
    }

    public PageResponse<BloodDonorResponse> listDonors(String q, BloodGroup bloodGroup, Pageable pageable) {
        if (StringUtils.hasText(q))
            return PageResponse.of(donorRepository.search(q, pageable).map(BloodDonorResponse::from));
        if (bloodGroup != null)
            return PageResponse.of(donorRepository.findByBloodGroupAndActiveTrue(bloodGroup, pageable)
                    .map(BloodDonorResponse::from));
        return PageResponse.of(donorRepository.findAll(pageable).map(BloodDonorResponse::from));
    }

    // ── Blood Units ───────────────────────────────────────────────────────────

    @Transactional
    public BloodUnitResponse addUnit(BloodUnitCreateRequest req) {
        LocalDate collection = req.collectionDate() != null ? req.collectionDate() : LocalDate.now();
        LocalDate expiry     = req.expiryDate() != null
                ? req.expiryDate()
                : collection.plusDays(req.componentType().defaultShelfDays());

        String donorName = req.donorName();
        if (req.donorId() != null) {
            BloodDonor donor = findDonorOrThrow(req.donorId());
            donorName = donor.getFirstName() + " " + donor.getLastName();
            donor.setLastDonationDate(collection);
            donor.setTotalDonations(donor.getTotalDonations() + 1);
            donorRepository.save(donor);
        }

        BloodUnit unit = BloodUnit.builder()
                .unitNumber(generateUnitNumber())
                .bloodGroup(req.bloodGroup())
                .donorId(req.donorId())
                .donorName(donorName)
                .componentType(req.componentType())
                .volumeMl(req.volumeMl() != null ? req.volumeMl() : req.componentType() == ComponentType.PLATELET_CONCENTRATE ? 50 : 450)
                .collectionDate(collection)
                .expiryDate(expiry)
                .notes(req.notes())
                .build();

        BloodUnit saved = unitRepository.save(unit);
        log.info("Blood unit {} added ({} {}, expires {})",
                saved.getUnitNumber(), saved.getBloodGroup().display(),
                saved.getComponentType(), saved.getExpiryDate());
        return BloodUnitResponse.from(saved);
    }

    public BloodUnitResponse getUnit(UUID id) {
        return BloodUnitResponse.from(findUnitOrThrow(id));
    }

    public PageResponse<BloodUnitResponse> listUnits(
            BloodGroup bloodGroup, ComponentType componentType, UnitStatus status, Pageable pageable) {
        if (bloodGroup != null && componentType != null && status != null)
            return PageResponse.of(unitRepository.findByBloodGroupAndComponentTypeAndStatus(
                    bloodGroup, componentType, status, pageable).map(BloodUnitResponse::from));
        if (bloodGroup != null && status != null)
            return PageResponse.of(unitRepository.findByBloodGroupAndStatus(bloodGroup, status, pageable)
                    .map(BloodUnitResponse::from));
        if (status != null)
            return PageResponse.of(unitRepository.findByStatus(status, pageable).map(BloodUnitResponse::from));
        return PageResponse.of(unitRepository.findAll(pageable).map(BloodUnitResponse::from));
    }

    @Transactional
    public BloodUnitResponse updateUnitStatus(UUID id, UpdateUnitStatusRequest req) {
        BloodUnit unit = findUnitOrThrow(id);
        if (unit.getStatus() == UnitStatus.ISSUED)
            throw ApiException.badRequest("UNIT_ISSUED", "Cannot change status of an issued unit");

        UnitStatus newStatus = req.status();
        if (newStatus == UnitStatus.AVAILABLE) {
            unit.setTestingStatus(TestingStatus.CLEARED);
        } else if (newStatus == UnitStatus.DISCARDED) {
            if (unit.getTestingStatus() == TestingStatus.PENDING)
                unit.setTestingStatus(TestingStatus.REJECTED);
        }
        unit.setStatus(newStatus);
        if (req.notes() != null) unit.setNotes(req.notes());

        return BloodUnitResponse.from(unitRepository.save(unit));
    }

    /** Returns matching available units sorted FEFO — used by the issue modal */
    public List<BloodUnitResponse> getAvailableUnits(BloodGroup bloodGroup, ComponentType componentType) {
        if (bloodGroup != null && componentType != null)
            return unitRepository.findByBloodGroupAndComponentTypeAndStatusOrderByExpiryDateAsc(
                    bloodGroup, componentType, UnitStatus.AVAILABLE)
                    .stream().map(BloodUnitResponse::from).toList();
        if (bloodGroup != null)
            return unitRepository.findByBloodGroupAndStatusOrderByExpiryDateAsc(bloodGroup, UnitStatus.AVAILABLE)
                    .stream().map(BloodUnitResponse::from).toList();
        return unitRepository.findByStatusOrderByExpiryDateAsc(UnitStatus.AVAILABLE)
                .stream().map(BloodUnitResponse::from).toList();
    }

    // ── Blood Requests ────────────────────────────────────────────────────────

    @Transactional
    public BloodRequestResponse createRequest(BloodRequestCreateRequest req) {
        BloodRequest request = BloodRequest.builder()
                .requestNumber(generateRequestNumber())
                .requestDate(LocalDate.now())
                .patientId(req.patientId())
                .patientName(req.patientName())
                .requestedBy(req.requestedBy())
                .bloodGroup(req.bloodGroup())
                .componentType(req.componentType())
                .unitsRequired(req.unitsRequired())
                .urgency(req.urgency() != null ? req.urgency() : BloodRequest.Urgency.ROUTINE)
                .requiredBy(req.requiredBy())
                .notes(req.notes())
                .build();
        BloodRequest saved = requestRepository.save(request);
        log.info("Blood request {} — {} {} for {}",
                saved.getRequestNumber(), req.unitsRequired(),
                req.bloodGroup().display(), req.patientName());
        return BloodRequestResponse.from(saved);
    }

    public BloodRequestResponse getRequest(UUID id) {
        return BloodRequestResponse.from(findRequestOrThrow(id));
    }

    public PageResponse<BloodRequestResponse> listRequests(RequestStatus status, Pageable pageable) {
        if (status != null)
            return PageResponse.of(requestRepository.findByStatus(status, pageable)
                    .map(BloodRequestResponse::from));
        return PageResponse.of(requestRepository.findAll(pageable).map(BloodRequestResponse::from));
    }

    @Transactional
    public BloodRequestResponse cancelRequest(UUID id) {
        BloodRequest request = findRequestOrThrow(id);
        if (request.getStatus() == RequestStatus.FULFILLED)
            throw ApiException.badRequest("REQUEST_FULFILLED", "Cannot cancel a fulfilled request");
        request.setStatus(RequestStatus.CANCELLED);
        return BloodRequestResponse.from(requestRepository.save(request));
    }

    // ── Blood Issue ───────────────────────────────────────────────────────────

    @Transactional
    public BloodIssueResponse issueBlood(BloodIssueRequest req) {
        BloodRequest request = findRequestOrThrow(req.requestId());
        BloodUnit    unit    = findUnitOrThrow(req.unitId());

        if (request.getStatus() == RequestStatus.FULFILLED)
            throw ApiException.badRequest("REQUEST_FULFILLED", "Request is already fully fulfilled");
        if (request.getStatus() == RequestStatus.CANCELLED)
            throw ApiException.badRequest("REQUEST_CANCELLED", "Cannot issue blood against a cancelled request");
        if (unit.getStatus() != UnitStatus.AVAILABLE)
            throw ApiException.badRequest("UNIT_NOT_AVAILABLE",
                    "Unit " + unit.getUnitNumber() + " is not available (status: " + unit.getStatus() + ")");
        if (unit.getBloodGroup() != request.getBloodGroup())
            throw ApiException.badRequest("BLOOD_GROUP_MISMATCH",
                    "Unit blood group " + unit.getBloodGroup().display() +
                    " does not match request " + request.getBloodGroup().display());
        if (unit.getComponentType() != request.getComponentType())
            throw ApiException.badRequest("COMPONENT_MISMATCH",
                    "Component type mismatch: unit=" + unit.getComponentType() +
                    ", request=" + request.getComponentType());
        if (unit.isExpired())
            throw ApiException.badRequest("UNIT_EXPIRED",
                    "Unit " + unit.getUnitNumber() + " expired on " + unit.getExpiryDate());

        BloodIssue issue = BloodIssue.builder()
                .issueNumber(generateIssueNumber())
                .issueDate(Instant.now())
                .requestId(request.getId())
                .requestNumber(request.getRequestNumber())
                .unitId(unit.getId())
                .unitNumber(unit.getUnitNumber())
                .bloodGroup(unit.getBloodGroup().name())
                .componentType(unit.getComponentType().name())
                .issuedTo(request.getPatientName())
                .issuedBy(req.issuedBy())
                .notes(req.notes())
                .build();

        unit.setStatus(UnitStatus.ISSUED);
        unitRepository.save(unit);

        request.setUnitsIssued(request.getUnitsIssued() + 1);
        request.setStatus(request.getUnitsIssued() >= request.getUnitsRequired()
                ? RequestStatus.FULFILLED : RequestStatus.PARTIALLY_FULFILLED);
        requestRepository.save(request);

        BloodIssue saved = issueRepository.save(issue);
        log.info("Blood issued {} — unit {} ({}) to {}",
                saved.getIssueNumber(), unit.getUnitNumber(),
                unit.getBloodGroup().display(), request.getPatientName());
        return BloodIssueResponse.from(saved);
    }

    public PageResponse<BloodIssueResponse> listIssues(Pageable pageable) {
        return PageResponse.of(issueRepository.findAllByOrderByIssueDateDesc(pageable)
                .map(BloodIssueResponse::from));
    }

    public List<BloodIssueResponse> getIssuesForRequest(UUID requestId) {
        return issueRepository.findByRequestId(requestId).stream()
                .map(BloodIssueResponse::from).toList();
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    public BloodBankDashboardResponse getDashboard() {
        Map<BloodGroup, Long> availMap  = new EnumMap<>(BloodGroup.class);
        Map<BloodGroup, Long> pendingMap = new EnumMap<>(BloodGroup.class);

        unitRepository.countByBloodGroupAndStatus(UnitStatus.AVAILABLE)
                .forEach(r -> availMap.put((BloodGroup) r[0], (Long) r[1]));
        unitRepository.countByBloodGroupAndStatus(UnitStatus.PENDING_TESTING)
                .forEach(r -> pendingMap.put((BloodGroup) r[0], (Long) r[1]));

        List<BloodBankDashboardResponse.BloodGroupStock> stockByGroup =
                Arrays.stream(BloodGroup.values()).map(bg ->
                        new BloodBankDashboardResponse.BloodGroupStock(
                                bg.name(), bg.display(),
                                availMap.getOrDefault(bg, 0L),
                                pendingMap.getOrDefault(bg, 0L)
                        )
                ).toList();

        long openRequests = requestRepository.countByStatusIn(
                List.of(RequestStatus.PENDING, RequestStatus.PARTIALLY_FULFILLED));

        return new BloodBankDashboardResponse(
                donorRepository.count(),
                donorRepository.countByActiveTrue(),
                unitRepository.countByStatus(UnitStatus.AVAILABLE),
                unitRepository.countByStatus(UnitStatus.PENDING_TESTING),
                openRequests,
                issueRepository.countToday(),
                stockByGroup
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BloodDonor   findDonorOrThrow(UUID id) {
        return donorRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("DONOR_NOT_FOUND", "Donor " + id + " not found"));
    }

    private BloodUnit findUnitOrThrow(UUID id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("UNIT_NOT_FOUND", "Blood unit " + id + " not found"));
    }

    private BloodRequest findRequestOrThrow(UUID id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("REQUEST_NOT_FOUND", "Blood request " + id + " not found"));
    }

    private String generateDonorNumber() {
        int year = LocalDate.now().getYear();
        return String.format("DON-%d-%05d", year, donorRepository.nextSequenceForYear(year));
    }

    private String generateUnitNumber() {
        int year = LocalDate.now().getYear();
        return String.format("BU-%d-%05d", year, unitRepository.nextSequenceForYear(year));
    }

    private String generateRequestNumber() {
        int year = LocalDate.now().getYear();
        return String.format("BRQ-%d-%05d", year, requestRepository.nextSequenceForYear(year));
    }

    private String generateIssueNumber() {
        int year = LocalDate.now().getYear();
        return String.format("BIS-%d-%05d", year, issueRepository.nextSequenceForYear(year));
    }
}
