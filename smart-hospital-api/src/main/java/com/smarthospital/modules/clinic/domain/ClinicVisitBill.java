package com.smarthospital.modules.clinic.domain;

import com.smarthospital.core.audit.AuditEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clinic_visit_bills")
public class ClinicVisitBill extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bill_number", nullable = false, unique = true)
    private String billNumber;

    @Column(name = "opd_visit_id", nullable = false)
    private UUID opdVisitId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<ClinicVisitBillItem> items = new ArrayList<>();

    public UUID getId()                         { return id; }
    public String getBillNumber()               { return billNumber; }
    public UUID getOpdVisitId()                 { return opdVisitId; }
    public UUID getPatientId()                  { return patientId; }
    public String getPatientName()              { return patientName; }
    public LocalDate getVisitDate()             { return visitDate; }
    public BigDecimal getTotalAmount()          { return totalAmount; }
    public String getStatus()                   { return status; }
    public List<ClinicVisitBillItem> getItems() { return items; }

    public void setBillNumber(String v)         { this.billNumber  = v; }
    public void setOpdVisitId(UUID v)           { this.opdVisitId  = v; }
    public void setPatientId(UUID v)            { this.patientId   = v; }
    public void setPatientName(String v)        { this.patientName = v; }
    public void setVisitDate(LocalDate v)       { this.visitDate   = v; }
    public void setTotalAmount(BigDecimal v)    { this.totalAmount = v; }
    public void setStatus(String v)             { this.status      = v; }
    public void setItems(List<ClinicVisitBillItem> v) { this.items = v; }
}
