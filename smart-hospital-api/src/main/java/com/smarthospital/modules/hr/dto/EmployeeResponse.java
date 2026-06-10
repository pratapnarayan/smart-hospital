package com.smarthospital.modules.hr.dto;

import com.smarthospital.modules.hr.domain.Employee;
import com.smarthospital.modules.hr.domain.Employee.EmployeeStatus;
import com.smarthospital.modules.hr.domain.Employee.EmploymentType;
import com.smarthospital.modules.hr.domain.Employee.Gender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
        UUID           id,
        String         employeeCode,
        String         firstName,
        String         lastName,
        LocalDate      dateOfBirth,
        Gender         gender,
        String         mobile,
        String         email,
        String         address,
        String         bloodGroup,
        UUID           departmentId,
        UUID           designationId,
        UUID           userId,
        EmploymentType employmentType,
        LocalDate      joinDate,
        EmployeeStatus status,
        String         profilePhoto,
        Instant        createdAt
) {
    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(
                e.getId(), e.getEmployeeCode(),
                e.getFirstName(), e.getLastName(),
                e.getDateOfBirth(), e.getGender(),
                e.getMobile(), e.getEmail(), e.getAddress(), e.getBloodGroup(),
                e.getDepartmentId(), e.getDesignationId(), e.getUserId(),
                e.getEmploymentType(), e.getJoinDate(), e.getStatus(),
                e.getProfilePhoto(), e.getCreatedAt()
        );
    }
}
