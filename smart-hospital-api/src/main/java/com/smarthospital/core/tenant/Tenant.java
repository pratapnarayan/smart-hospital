package com.smarthospital.core.tenant;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Registry entry in public.tenants.
 *
 * IMPORTANT: schema = "public" ensures Hibernate generates fully-qualified SQL
 * (SELECT … FROM public.tenants) so queries always hit the right table
 * regardless of which search_path is active on the current connection.
 */
@Entity
@Table(name = "tenants", schema = "public",
       uniqueConstraints = @UniqueConstraint(name = "uq_tenants_schema_name",
                                             columnNames = "schema_name"))
@EntityListeners(AuditingEntityListener.class)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    /** PostgreSQL schema name, e.g. "hospital_001". Max 63 chars (PG limit). */
    @Column(name = "schema_name", nullable = false, unique = true, length = 63)
    private String schemaName;

    @Column(nullable = false, length = 50)
    private String plan = "BASIC";

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "clinic_type", nullable = false, length = 20)
    private String clinicType = "FULL_HMS";

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Tenant() {}

    // Getters
    public UUID    getId()         { return id; }
    public String  getName()       { return name; }
    public String  getSchemaName() { return schemaName; }
    public String  getPlan()       { return plan; }
    public String  getStatus()     { return status; }
    public String  getClinicType() { return clinicType; }
    public Instant getCreatedAt()  { return createdAt; }
    public Instant getUpdatedAt()  { return updatedAt; }

    // Setters
    public void setName(String v)       { this.name       = v; }
    public void setPlan(String v)       { this.plan       = v; }
    public void setStatus(String v)     { this.status     = v; }
    public void setClinicType(String v) { this.clinicType = v; }

    // Builder
    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private final Tenant t = new Tenant();
        public Builder name(String v)       { t.name       = v; return this; }
        public Builder schemaName(String v) { t.schemaName = v; return this; }
        public Builder plan(String v)       { t.plan       = v; return this; }
        public Builder status(String v)     { t.status     = v; return this; }
        public Builder clinicType(String v) { t.clinicType = v; return this; }
        public Tenant build()               { return t; }
    }
}
