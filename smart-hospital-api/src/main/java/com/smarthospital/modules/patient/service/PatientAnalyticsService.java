package com.smarthospital.modules.patient.service;

import com.smarthospital.modules.analytics.dto.*;
import com.smarthospital.modules.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PatientAnalyticsService {

    private final PatientRepository patientRepo;

    public PatientAnalyticsService(PatientRepository patientRepo) {
        this.patientRepo = patientRepo;
    }

    public PatientAnalyticsResponse getAnalytics(LocalDate from, LocalDate to) {
        long total = patientRepo.countTotal();
        long newPatients = patientRepo.countRegisteredBetween(from, to);
        // Returning = patients with >1 visit — approximate as total minus new this period
        long returning = Math.max(0, total - newPatients);
        double retention = total > 0 ? (double) returning / total * 100 : 0;

        return new PatientAnalyticsResponse(
                total,
                newPatients,
                returning,
                retention,
                patientRepo.countByMonth(from, to).stream()
                        .map(r -> new TrendPoint(r[0].toString(), ((Number) r[1]).doubleValue()))
                        .toList(),
                patientRepo.countByGender().stream()
                        .map(r -> new NameValuePoint(r[0].toString(), ((Number) r[1]).doubleValue()))
                        .toList(),
                buildAgeDistribution(),
                patientRepo.countByBloodGroup().stream()
                        .map(r -> new NameValuePoint(r[0] != null ? r[0].toString() : "Unknown",
                                ((Number) r[1]).doubleValue()))
                        .toList(),
                List.of() // patientsByDepartment — not available in patient domain
        );
    }

    public long getTotalPatients() {
        return patientRepo.countTotal();
    }

    private List<NameValuePoint> buildAgeDistribution() {
        return patientRepo.countByAgeBracket().stream()
                .map(r -> new NameValuePoint(r[0].toString(), ((Number) r[1]).doubleValue()))
                .toList();
    }
}
