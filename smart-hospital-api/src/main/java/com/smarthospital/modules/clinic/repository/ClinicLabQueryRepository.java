package com.smarthospital.modules.clinic.repository;

import com.smarthospital.modules.pathology.domain.LabOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Read-only query repository for LabOrder used by the Clinic billing module.
 * Spring Data JPA supports multiple repository interfaces for the same entity.
 * Finds lab orders linked to an OPD visit via the sourceId field.
 */
public interface ClinicLabQueryRepository extends JpaRepository<LabOrder, UUID> {

    List<LabOrder> findBySourceId(UUID sourceId);
}
