package com.smarthospital.modules.doctor.dto;

import com.smarthospital.modules.doctor.domain.DoctorSchedule;

import java.time.LocalTime;
import java.util.UUID;

public record DoctorScheduleResponse(UUID id, String dayOfWeek, LocalTime shiftStart, LocalTime shiftEnd, int slotDurationMins, boolean active) {
    public static DoctorScheduleResponse from(DoctorSchedule s) {
        return new DoctorScheduleResponse(s.getId(), s.getDayOfWeek().name(), s.getShiftStart(), s.getShiftEnd(), s.getSlotDurationMins(), s.isActive());
    }
}
