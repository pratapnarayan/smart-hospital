package com.smarthospital.modules.doctor.domain;

import com.smarthospital.core.audit.AuditEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "doctor_profiles", indexes = {
    @Index(name = "idx_doctor_profiles_employee_id", columnList = "employee_id")
})
public class DoctorProfile extends AuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "employee_id", nullable = false, unique = true)
    private UUID employeeId;
    @Column(name = "profile_photo", length = 500)
    private String profilePhoto;
    @Column(columnDefinition = "TEXT")
    private String biography;
    @Column(columnDefinition = "TEXT")
    private String qualifications;
    @Column(name = "experience_years")
    private Integer experienceYears = 0;
    @Column(name = "consultation_fee", precision = 10, scale = 2)
    private BigDecimal consultationFee = BigDecimal.ZERO;
    @Column(name = "follow_up_fee", precision = 10, scale = 2)
    private BigDecimal followUpFee = BigDecimal.ZERO;
    @Column(name = "tele_consultation_fee", precision = 10, scale = 2)
    private BigDecimal teleConsultationFee = BigDecimal.ZERO;
    @Column(length = 300)
    private String languages;
    @Column(columnDefinition = "TEXT")
    private String awards;
    @Column(columnDefinition = "TEXT")
    private String achievements;
    @Column(columnDefinition = "TEXT")
    private String publications;
    @Column(name = "online_booking_enabled", nullable = false)
    private boolean onlineBookingEnabled = true;
    @Column(name = "display_on_portal", nullable = false)
    private boolean displayOnPortal = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "doctor_specializations",
        joinColumns = @JoinColumn(name = "doctor_profile_id"),
        inverseJoinColumns = @JoinColumn(name = "specialization_id"))
    private Set<Specialization> specializations = new HashSet<>();

    protected DoctorProfile() {}
    public UUID getId()               { return id; }
    public UUID getEmployeeId()       { return employeeId; }
    public String getProfilePhoto()   { return profilePhoto; }
    public String getBiography()      { return biography; }
    public String getQualifications() { return qualifications; }
    public Integer getExperienceYears()      { return experienceYears; }
    public BigDecimal getConsultationFee()   { return consultationFee; }
    public BigDecimal getFollowUpFee()       { return followUpFee; }
    public BigDecimal getTeleConsultationFee(){ return teleConsultationFee; }
    public String getLanguages()      { return languages; }
    public String getAwards()         { return awards; }
    public String getAchievements()   { return achievements; }
    public String getPublications()   { return publications; }
    public boolean isOnlineBookingEnabled(){ return onlineBookingEnabled; }
    public boolean isDisplayOnPortal()     { return displayOnPortal; }
    public Set<Specialization> getSpecializations(){ return specializations; }
    public void setProfilePhoto(String v)     { this.profilePhoto = v; }
    public void setBiography(String v)        { this.biography = v; }
    public void setQualifications(String v)   { this.qualifications = v; }
    public void setExperienceYears(Integer v) { this.experienceYears = v; }
    public void setConsultationFee(BigDecimal v)      { this.consultationFee = v; }
    public void setFollowUpFee(BigDecimal v)           { this.followUpFee = v; }
    public void setTeleConsultationFee(BigDecimal v)   { this.teleConsultationFee = v; }
    public void setLanguages(String v)        { this.languages = v; }
    public void setAwards(String v)           { this.awards = v; }
    public void setAchievements(String v)     { this.achievements = v; }
    public void setPublications(String v)     { this.publications = v; }
    public void setOnlineBookingEnabled(boolean v){ this.onlineBookingEnabled = v; }
    public void setDisplayOnPortal(boolean v)     { this.displayOnPortal = v; }
    public void setSpecializations(Set<Specialization> v){ this.specializations = v; }

    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private final DoctorProfile p = new DoctorProfile();
        public Builder employeeId(UUID v)            { p.employeeId = v; return this; }
        public Builder profilePhoto(String v)        { p.profilePhoto = v; return this; }
        public Builder biography(String v)           { p.biography = v; return this; }
        public Builder qualifications(String v)      { p.qualifications = v; return this; }
        public Builder experienceYears(Integer v)    { p.experienceYears = v; return this; }
        public Builder consultationFee(BigDecimal v) { p.consultationFee = v; return this; }
        public Builder followUpFee(BigDecimal v)     { p.followUpFee = v; return this; }
        public Builder teleConsultationFee(BigDecimal v){ p.teleConsultationFee = v; return this; }
        public Builder languages(String v)           { p.languages = v; return this; }
        public Builder onlineBookingEnabled(boolean v){ p.onlineBookingEnabled = v; return this; }
        public Builder displayOnPortal(boolean v)    { p.displayOnPortal = v; return this; }
        public DoctorProfile build()                 { return p; }
    }
}
