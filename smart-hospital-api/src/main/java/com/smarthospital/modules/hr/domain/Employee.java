package com.smarthospital.modules.hr.domain;

import com.smarthospital.core.audit.AuditEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
    name = "employees",
    indexes = {
        @Index(name = "idx_emp_dept",   columnList = "department_id"),
        @Index(name = "idx_emp_status", columnList = "status"),
        @Index(name = "idx_emp_mobile", columnList = "mobile")
    }
)
@SQLDelete(sql = "UPDATE employees SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Employee extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 30)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 15)
    private String mobile;

    @Column(length = 150)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "designation_id")
    private UUID designationId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Column(name = "profile_photo", length = 500)
    private String profilePhoto;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum Gender         { MALE, FEMALE, OTHER }
    public enum EmploymentType { FULL_TIME, PART_TIME, CONTRACT, CONSULTANT }
    public enum EmployeeStatus { ACTIVE, ON_LEAVE, SUSPENDED, RESIGNED, TERMINATED }

    protected Employee() {}

    public UUID           getId()             { return id; }
    public String         getEmployeeCode()   { return employeeCode; }
    public String         getFirstName()      { return firstName; }
    public String         getLastName()       { return lastName; }
    public LocalDate      getDateOfBirth()    { return dateOfBirth; }
    public Gender         getGender()         { return gender; }
    public String         getMobile()         { return mobile; }
    public String         getEmail()          { return email; }
    public String         getAddress()        { return address; }
    public String         getBloodGroup()     { return bloodGroup; }
    public UUID           getDepartmentId()   { return departmentId; }
    public UUID           getDesignationId()  { return designationId; }
    public UUID           getUserId()         { return userId; }
    public EmploymentType getEmploymentType() { return employmentType; }
    public LocalDate      getJoinDate()       { return joinDate; }
    public EmployeeStatus getStatus()         { return status; }
    public String         getProfilePhoto()   { return profilePhoto; }

    public void setFirstName(String v)           { this.firstName      = v; }
    public void setLastName(String v)            { this.lastName       = v; }
    public void setDateOfBirth(LocalDate v)      { this.dateOfBirth    = v; }
    public void setGender(Gender v)              { this.gender         = v; }
    public void setMobile(String v)              { this.mobile         = v; }
    public void setEmail(String v)               { this.email          = v; }
    public void setAddress(String v)             { this.address        = v; }
    public void setBloodGroup(String v)          { this.bloodGroup     = v; }
    public void setDepartmentId(UUID v)          { this.departmentId   = v; }
    public void setDesignationId(UUID v)         { this.designationId  = v; }
    public void setUserId(UUID v)                { this.userId         = v; }
    public void setEmploymentType(EmploymentType v){ this.employmentType= v; }
    public void setJoinDate(LocalDate v)         { this.joinDate       = v; }
    public void setStatus(EmployeeStatus v)      { this.status         = v; }
    public void setProfilePhoto(String v)        { this.profilePhoto   = v; }

    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private final Employee e = new Employee();
        public Builder employeeCode(String v)        { e.employeeCode   = v; return this; }
        public Builder firstName(String v)           { e.firstName      = v; return this; }
        public Builder lastName(String v)            { e.lastName       = v; return this; }
        public Builder dateOfBirth(LocalDate v)      { e.dateOfBirth    = v; return this; }
        public Builder gender(Gender v)              { e.gender         = v; return this; }
        public Builder mobile(String v)              { e.mobile         = v; return this; }
        public Builder email(String v)               { e.email          = v; return this; }
        public Builder address(String v)             { e.address        = v; return this; }
        public Builder bloodGroup(String v)          { e.bloodGroup     = v; return this; }
        public Builder departmentId(UUID v)          { e.departmentId   = v; return this; }
        public Builder designationId(UUID v)         { e.designationId  = v; return this; }
        public Builder userId(UUID v)                { e.userId         = v; return this; }
        public Builder employmentType(EmploymentType v){ e.employmentType= v; return this; }
        public Builder joinDate(LocalDate v)         { e.joinDate       = v; return this; }
        public Builder profilePhoto(String v)        { e.profilePhoto   = v; return this; }
        public Employee build()                      { return e; }
    }
}
