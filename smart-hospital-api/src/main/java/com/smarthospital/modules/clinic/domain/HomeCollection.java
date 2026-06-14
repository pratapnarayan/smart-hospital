package com.smarthospital.modules.clinic.domain;

import com.smarthospital.core.audit.AuditEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clinic_home_collections")
public class HomeCollection extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "patient_phone")
    private String patientPhone;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "collected_at")
    private Instant collectedAt;

    @Column(name = "technician_id")
    private UUID technicianId;

    @Column(name = "technician_name")
    private String technicianName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CollectionStatus status = CollectionStatus.SCHEDULED;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "notes")
    private String notes;

    // Getters
    public UUID             getId()             { return id; }
    public UUID             getPatientId()      { return patientId; }
    public String           getPatientName()    { return patientName; }
    public String           getPatientPhone()   { return patientPhone; }
    public String           getAddress()        { return address; }
    public Instant          getScheduledAt()    { return scheduledAt; }
    public Instant          getCollectedAt()    { return collectedAt; }
    public UUID             getTechnicianId()   { return technicianId; }
    public String           getTechnicianName() { return technicianName; }
    public CollectionStatus getStatus()         { return status; }
    public String           getFailureReason()  { return failureReason; }
    public String           getNotes()          { return notes; }

    // Setters
    public void setPatientId(UUID v)            { this.patientId      = v; }
    public void setPatientName(String v)        { this.patientName    = v; }
    public void setPatientPhone(String v)       { this.patientPhone   = v; }
    public void setAddress(String v)            { this.address        = v; }
    public void setScheduledAt(Instant v)       { this.scheduledAt    = v; }
    public void setCollectedAt(Instant v)       { this.collectedAt    = v; }
    public void setTechnicianId(UUID v)         { this.technicianId   = v; }
    public void setTechnicianName(String v)     { this.technicianName = v; }
    public void setStatus(CollectionStatus v)   { this.status         = v; }
    public void setFailureReason(String v)      { this.failureReason  = v; }
    public void setNotes(String v)              { this.notes          = v; }
}
