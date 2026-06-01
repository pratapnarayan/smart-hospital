package com.smarthospital.modules.patient.service;

import com.smarthospital.core.exception.ApiException;
import com.smarthospital.core.pagination.PageResponse;
import com.smarthospital.modules.patient.domain.Patient;
import com.smarthospital.modules.patient.dto.PatientCreateRequest;
import com.smarthospital.modules.patient.dto.PatientResponse;
import com.smarthospital.modules.patient.repository.PatientRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public PageResponse<PatientResponse> search(String query, Pageable pageable) {
        if (StringUtils.hasText(query)) {
            // The native FTS query owns its own ORDER BY (ts_rank relevance).
            // Passing a sorted Pageable would make Spring Data append a second
            // ORDER BY, which PostgreSQL rejects with SQLState 42601.
            PageRequest unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            return PageResponse.of(patientRepository.fullTextSearch(query, unsorted).map(PatientResponse::from));
        }
        return PageResponse.of(patientRepository.findAll(pageable).map(PatientResponse::from));
    }

    public PatientResponse findById(UUID id) {
        return patientRepository.findById(id)
                .map(PatientResponse::from)
                .orElseThrow(() -> ApiException.notFound("PATIENT_NOT_FOUND",
                        "Patient with ID " + id + " not found"));
    }

    @Transactional
    public PatientResponse create(PatientCreateRequest req) {
        if (StringUtils.hasText(req.mobile()) && patientRepository.existsByMobile(req.mobile())) {
            throw ApiException.conflict("DUPLICATE_MOBILE", "A patient with mobile " + req.mobile() + " already exists");
        }
        Patient patient = Patient.builder()
                .firstName(req.firstName())
                .lastName(req.lastName())
                .dateOfBirth(req.dateOfBirth())
                .gender(req.gender())
                .mobile(req.mobile())
                .email(req.email())
                .address(req.address())
                .bloodGroup(req.bloodGroup())
                .guardianName(req.guardianName())
                .guardianMobile(req.guardianMobile())
                .build();
        return PatientResponse.from(patientRepository.save(patient));
    }

    @Transactional
    public PatientResponse update(UUID id, PatientCreateRequest req) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("PATIENT_NOT_FOUND", "Patient with ID " + id + " not found"));
        patient.setFirstName(req.firstName());
        patient.setLastName(req.lastName());
        patient.setDateOfBirth(req.dateOfBirth());
        patient.setGender(req.gender());
        patient.setMobile(req.mobile());
        patient.setEmail(req.email());
        patient.setAddress(req.address());
        patient.setBloodGroup(req.bloodGroup());
        patient.setGuardianName(req.guardianName());
        return PatientResponse.from(patientRepository.save(patient));
    }

    @Transactional
    public void delete(UUID id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("PATIENT_NOT_FOUND", "Patient with ID " + id + " not found"));
        patientRepository.delete(patient);
    }
}
