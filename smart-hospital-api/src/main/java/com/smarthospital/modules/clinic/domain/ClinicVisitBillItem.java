package com.smarthospital.modules.clinic.domain;

import com.smarthospital.core.audit.AuditEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "clinic_visit_bill_items")
public class ClinicVisitBillItem extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private ClinicVisitBill bill;

    @Column(name = "line_type", nullable = false, length = 30)
    private String lineType;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "source_id")
    private UUID sourceId;

    public UUID getId()              { return id; }
    public ClinicVisitBill getBill() { return bill; }
    public String getLineType()      { return lineType; }
    public String getDescription()   { return description; }
    public BigDecimal getAmount()    { return amount; }
    public UUID getSourceId()        { return sourceId; }

    public void setBill(ClinicVisitBill v)  { this.bill        = v; }
    public void setLineType(String v)       { this.lineType    = v; }
    public void setDescription(String v)    { this.description = v; }
    public void setAmount(BigDecimal v)     { this.amount      = v; }
    public void setSourceId(UUID v)         { this.sourceId    = v; }
}
