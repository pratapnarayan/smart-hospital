package com.smarthospital.modules.clinic.service;

import com.smarthospital.core.exception.ApiException;
import com.smarthospital.modules.clinic.domain.CollectionStatus;
import com.smarthospital.modules.clinic.domain.HomeCollection;
import com.smarthospital.modules.clinic.dto.*;
import com.smarthospital.modules.clinic.repository.HomeCollectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HomeCollectionService {

    private final HomeCollectionRepository repository;

    private static final Map<CollectionStatus, Set<CollectionStatus>> VALID_TRANSITIONS = Map.of(
            CollectionStatus.SCHEDULED, Set.of(CollectionStatus.EN_ROUTE, CollectionStatus.CANCELLED),
            CollectionStatus.EN_ROUTE,  Set.of(CollectionStatus.COLLECTED, CollectionStatus.FAILED, CollectionStatus.CANCELLED)
    );

    public HomeCollectionService(HomeCollectionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public HomeCollectionResponse create(HomeCollectionCreateRequest req) {
        HomeCollection h = new HomeCollection();
        h.setPatientId(req.patientId());
        h.setPatientName(req.patientName());
        h.setPatientPhone(req.patientPhone());
        h.setAddress(req.address());
        h.setScheduledAt(req.scheduledAt());
        h.setTechnicianId(req.technicianId());
        h.setTechnicianName(req.technicianName());
        h.setNotes(req.notes());
        h.setStatus(CollectionStatus.SCHEDULED);
        return HomeCollectionResponse.from(repository.save(h));
    }

    @Transactional(readOnly = true)
    public HomeCollectionSummaryResponse findByDate(LocalDate date) {
        Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<HomeCollection> list = repository.findByScheduledDate(start, end);
        return toSummary(list);
    }

    @Transactional(readOnly = true)
    public HomeCollectionSummaryResponse findByTechnicianAndDate(UUID technicianId, LocalDate date) {
        Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<HomeCollection> list = repository.findByTechnicianAndDate(technicianId, start, end);
        return toSummary(list);
    }

    @Transactional(readOnly = true)
    public HomeCollectionResponse findById(UUID id) {
        return HomeCollectionResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<HomeCollectionResponse> findByPatient(UUID patientId) {
        return repository.findByPatientIdOrderByScheduledAtDesc(patientId)
                .stream().map(HomeCollectionResponse::from).toList();
    }

    @Transactional
    public HomeCollectionResponse reschedule(UUID id, HomeCollectionUpdateRequest req) {
        HomeCollection h = getOrThrow(id);
        if (h.getStatus() != CollectionStatus.SCHEDULED) {
            throw ApiException.badRequest("INVALID_STATE",
                    "Cannot reschedule a collection with status " + h.getStatus());
        }
        h.setScheduledAt(req.scheduledAt());
        if (req.technicianId() != null) h.setTechnicianId(req.technicianId());
        if (req.technicianName() != null) h.setTechnicianName(req.technicianName());
        if (req.notes() != null) h.setNotes(req.notes());
        return HomeCollectionResponse.from(repository.save(h));
    }

    @Transactional
    public HomeCollectionResponse updateStatus(UUID id, HomeCollectionStatusRequest req) {
        HomeCollection h = getOrThrow(id);
        CollectionStatus next = req.status();  // already validated by Jackson
        Set<CollectionStatus> allowed = VALID_TRANSITIONS.getOrDefault(h.getStatus(), Set.of());
        if (!allowed.contains(next)) {
            throw ApiException.badRequest("INVALID_TRANSITION",
                    "Invalid status transition from " + h.getStatus() + " to " + next);
        }
        if (next == CollectionStatus.FAILED) {
            if (req.failureReason() == null || req.failureReason().isBlank()) {
                throw ApiException.badRequest("REQUIRED_FIELD", "Failure reason is required");
            }
            h.setFailureReason(req.failureReason());
        }
        h.setStatus(next);
        if (next == CollectionStatus.COLLECTED) {
            h.setCollectedAt(Instant.now());
        }
        return HomeCollectionResponse.from(repository.save(h));
    }

    private HomeCollection getOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("HOME_COLLECTION_NOT_FOUND",
                        "Home collection not found: " + id));
    }

    private HomeCollectionSummaryResponse toSummary(List<HomeCollection> list) {
        Map<String, Long> byStatus = list.stream()
                .collect(Collectors.groupingBy(h -> h.getStatus().name(), Collectors.counting()));
        List<HomeCollectionResponse> dtos = list.stream().map(HomeCollectionResponse::from).toList();
        return new HomeCollectionSummaryResponse(list.size(), byStatus, dtos);
    }
}
