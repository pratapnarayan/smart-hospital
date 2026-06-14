package com.smarthospital.modules.clinic.service;

import com.smarthospital.modules.clinic.dto.ClinicQuickRegisterRequest;
import com.smarthospital.modules.patient.domain.Patient.Gender;
import com.smarthospital.modules.patient.dto.PatientCreateRequest;
import com.smarthospital.modules.patient.dto.PatientResponse;
import com.smarthospital.modules.patient.service.PatientService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ClinicPatientService {

    private final PatientService patientService;

    public ClinicPatientService(PatientService patientService) {
        this.patientService = patientService;
    }

    public PatientResponse quickRegister(ClinicQuickRegisterRequest req) {
        String[] parts = req.name().trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "LNU";

        int ageYears = req.age() != null ? req.age() : 0;
        LocalDate dob = ageYears > 0
                ? LocalDate.now().minusYears(ageYears)
                : LocalDate.now().minusDays(1);  // age unknown/0 → set DOB to yesterday to satisfy @Past

        Gender gender;
        try {
            gender = req.gender() != null ? Gender.valueOf(req.gender().toUpperCase()) : Gender.OTHER;
        } catch (IllegalArgumentException e) {
            gender = Gender.OTHER;
        }

        // Component order from PatientCreateRequest.java:
        // firstName, lastName, dateOfBirth, gender, mobile, email, address, bloodGroup, guardianName, guardianMobile
        PatientCreateRequest createReq = new PatientCreateRequest(
                firstName,
                lastName,
                dob,
                gender,
                req.phone(),
                null,   // email
                null,   // address
                null,   // bloodGroup
                null,   // guardianName
                null    // guardianMobile
        );
        return patientService.create(createReq);
    }
}
