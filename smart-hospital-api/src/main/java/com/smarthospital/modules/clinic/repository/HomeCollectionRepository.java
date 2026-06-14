package com.smarthospital.modules.clinic.repository;

import com.smarthospital.modules.clinic.domain.CollectionStatus;
import com.smarthospital.modules.clinic.domain.HomeCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HomeCollectionRepository extends JpaRepository<HomeCollection, UUID> {

    @Query("SELECT h FROM HomeCollection h WHERE h.scheduledAt >= :startOfDay AND h.scheduledAt < :endOfDay ORDER BY h.scheduledAt")
    List<HomeCollection> findByScheduledDate(@Param("startOfDay") Instant startOfDay,
                                              @Param("endOfDay") Instant endOfDay);

    @Query("SELECT h FROM HomeCollection h WHERE h.technicianId = :technicianId AND h.scheduledAt >= :startOfDay AND h.scheduledAt < :endOfDay ORDER BY h.scheduledAt")
    List<HomeCollection> findByTechnicianAndDate(@Param("technicianId") UUID technicianId,
                                                  @Param("startOfDay") Instant startOfDay,
                                                  @Param("endOfDay") Instant endOfDay);

    List<HomeCollection> findByPatientIdOrderByScheduledAtDesc(UUID patientId);

    long countByStatusAndScheduledAtBetween(CollectionStatus status, Instant start, Instant end);
}
