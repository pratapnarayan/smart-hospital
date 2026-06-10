package com.smarthospital.modules.doctor.domain;

import com.smarthospital.core.audit.CreatedOnlyAuditEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "specializations")
public class Specialization extends CreatedOnlyAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Column(nullable = false, unique = true, length = 20)
    private String code;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false)
    private boolean active = true;

    protected Specialization() {}
    public UUID getId()           { return id; }
    public String getName()       { return name; }
    public String getCode()       { return code; }
    public String getDescription(){ return description; }
    public boolean isActive()     { return active; }
    public void setName(String v)        { this.name = v; }
    public void setCode(String v)        { this.code = v; }
    public void setDescription(String v) { this.description = v; }
    public void setActive(boolean v)     { this.active = v; }

    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private final Specialization s = new Specialization();
        public Builder name(String v)        { s.name = v; return this; }
        public Builder code(String v)        { s.code = v; return this; }
        public Builder description(String v) { s.description = v; return this; }
        public Builder active(boolean v)     { s.active = v; return this; }
        public Specialization build()        { return s; }
    }
}
