package com.smarthospital.modules.doctor.repository;

import com.smarthospital.modules.doctor.domain.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpecializationRepository extends JpaRepository<Specialization, UUID> {
    boolean existsByNameIgnoreCase(String name);
    List<Specialization> findAllByActiveTrue();
}
