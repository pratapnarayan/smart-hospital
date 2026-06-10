package com.smarthospital.modules.doctor.domain;

import com.smarthospital.core.audit.CreatedOnlyAuditEntity;
import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "doctor_schedules", indexes = {
    @Index(name = "idx_doctor_schedules_doctor_id", columnList = "doctor_id")
})
public class DoctorSchedule extends CreatedOnlyAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;
    @Column(name = "shift_start", nullable = false)
    private LocalTime shiftStart;
    @Column(name = "shift_end", nullable = false)
    private LocalTime shiftEnd;
    @Column(name = "slot_duration_mins", nullable = false)
    private int slotDurationMins = 15;
    @Column(nullable = false)
    private boolean active = true;

    public enum DayOfWeek { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

    protected DoctorSchedule() {}
    public UUID getId()             { return id; }
    public UUID getDoctorId()       { return doctorId; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getShiftStart(){ return shiftStart; }
    public LocalTime getShiftEnd()  { return shiftEnd; }
    public int getSlotDurationMins(){ return slotDurationMins; }
    public boolean isActive()       { return active; }
    public void setDayOfWeek(DayOfWeek v)  { this.dayOfWeek = v; }
    public void setShiftStart(LocalTime v) { this.shiftStart = v; }
    public void setShiftEnd(LocalTime v)   { this.shiftEnd = v; }
    public void setSlotDurationMins(int v) { this.slotDurationMins = v; }
    public void setActive(boolean v)       { this.active = v; }

    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private final DoctorSchedule s = new DoctorSchedule();
        public Builder doctorId(UUID v)         { s.doctorId = v; return this; }
        public Builder dayOfWeek(DayOfWeek v)   { s.dayOfWeek = v; return this; }
        public Builder shiftStart(LocalTime v)  { s.shiftStart = v; return this; }
        public Builder shiftEnd(LocalTime v)    { s.shiftEnd = v; return this; }
        public Builder slotDurationMins(int v)  { s.slotDurationMins = v; return this; }
        public Builder active(boolean v)        { s.active = v; return this; }
        public DoctorSchedule build()           { return s; }
    }
}
