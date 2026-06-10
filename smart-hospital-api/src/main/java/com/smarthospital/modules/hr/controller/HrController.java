package com.smarthospital.modules.hr.controller;

import com.smarthospital.core.pagination.PageResponse;
import com.smarthospital.modules.hr.domain.LeaveRequest.LeaveStatus;
import com.smarthospital.modules.hr.dto.*;
import com.smarthospital.modules.hr.service.HrService;
import com.smarthospital.shared.dto.ApiResponse;
import com.smarthospital.shared.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hr")
@Tag(name = "HR", description = "Employees, departments, attendance and leave management")
public class HrController {

    private final HrService hrService;
    private final FileStorageService fileStorage;

    public HrController(HrService hrService, FileStorageService fileStorage) {
        this.hrService   = hrService;
        this.fileStorage = fileStorage;
    }

    // ── Departments ───────────────────────────────────────────────────────────

    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('HR.VIEW')")
    @Operation(summary = "List active departments")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> listDepartments() {
        return ResponseEntity.ok(ApiResponse.ok(hrService.listDepartments()));
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAuthority('HR.MANAGE')")
    @Operation(summary = "Create a department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(hrService.createDepartment(request)));
    }

    // ── Designations ──────────────────────────────────────────────────────────

    @GetMapping("/designations")
    @PreAuthorize("hasAuthority('HR.VIEW')")
    @Operation(summary = "List designations, optionally filter by department")
    public ResponseEntity<ApiResponse<List<DesignationResponse>>> listDesignations(
            @RequestParam(required = false) UUID departmentId) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.listDesignations(departmentId)));
    }

    @PostMapping("/designations")
    @PreAuthorize("hasAuthority('HR.MANAGE')")
    @Operation(summary = "Create a designation")
    public ResponseEntity<ApiResponse<DesignationResponse>> createDesignation(
            @Valid @RequestBody DesignationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(hrService.createDesignation(request)));
    }

    // ── Employees ─────────────────────────────────────────────────────────────

    @PostMapping("/employees")
    @PreAuthorize("hasAuthority('HR.CREATE')")
    @Operation(summary = "Add a new employee")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(hrService.createEmployee(request)));
    }

    @GetMapping("/employees/{id}")
    @PreAuthorize("hasAuthority('HR.VIEW')")
    @Operation(summary = "Get employee details")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.getEmployee(id)));
    }

    @GetMapping("/employees")
    @PreAuthorize("hasAuthority('HR.VIEW')")
    @Operation(summary = "List employees, optionally filter by department")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> listEmployees(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("firstName").ascending());
        if (search != null && !search.isBlank())
            return ResponseEntity.ok(ApiResponse.ok(hrService.searchEmployees(search, pageable)));
        return ResponseEntity.ok(ApiResponse.ok(hrService.listEmployees(departmentId, pageable)));
    }

    @PatchMapping("/employees/{id}")
    @PreAuthorize("hasAuthority('HR.EDIT')")
    @Operation(summary = "Update employee details or status")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.updateEmployee(id, request)));
    }

    @PostMapping(value = "/employees/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('HR.EDIT')")
    @Operation(summary = "Upload employee profile photo")
    public ResponseEntity<ApiResponse<EmployeeResponse>> uploadEmployeePhoto(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        hrService.getEmployee(id);  // verify exists → 404 before writing to storage
        String url = fileStorage.store(file, "emp_" + id);
        try {
            return ResponseEntity.ok(ApiResponse.ok(hrService.updateEmployeePhoto(id, url)));
        } catch (Exception e) {
            fileStorage.delete(url);
            throw e;
        }
    }

    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasAuthority('HR.MANAGE')")
    @Operation(summary = "Soft-delete an employee")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable UUID id) {
        hrService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    @PostMapping("/attendance")
    @PreAuthorize("hasAuthority('HR.EDIT')")
    @Operation(summary = "Mark or update attendance for an employee")
    public ResponseEntity<ApiResponse<AttendanceResponse>> markAttendance(
            @Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.markAttendance(request)));
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasAuthority('HR.VIEW')")
    @Operation(summary = "List attendance records for a date (defaults to today)")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAttendanceByDate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.getAttendanceByDate(date)));
    }

    @GetMapping("/employees/{id}/attendance")
    @PreAuthorize("hasAuthority('HR.VIEW')")
    @Operation(summary = "Get attendance history for an employee")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getEmployeeAttendance(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.getEmployeeAttendance(id)));
    }

    // ── Leave Requests ────────────────────────────────────────────────────────

    @PostMapping("/leave")
    @PreAuthorize("hasAuthority('HR.CREATE')")
    @Operation(summary = "Apply for leave")
    public ResponseEntity<ApiResponse<LeaveRequestDto.Response>> applyLeave(
            @Valid @RequestBody LeaveRequestDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(hrService.applyLeave(request)));
    }

    @GetMapping("/leave")
    @PreAuthorize("hasAuthority('HR.VIEW')")
    @Operation(summary = "List leave requests, filter by employee or status")
    public ResponseEntity<ApiResponse<PageResponse<LeaveRequestDto.Response>>> listLeaves(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(hrService.listLeaves(employeeId, status, pageable)));
    }

    @PostMapping("/leave/{id}/approve")
    @PreAuthorize("hasAuthority('HR.MANAGE')")
    @Operation(summary = "Approve a leave request")
    public ResponseEntity<ApiResponse<LeaveRequestDto.Response>> approveLeave(
            @PathVariable UUID id,
            @RequestBody LeaveRequestDto.ApproveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.approveLeave(id, request)));
    }

    @PostMapping("/leave/{id}/reject")
    @PreAuthorize("hasAuthority('HR.MANAGE')")
    @Operation(summary = "Reject a leave request")
    public ResponseEntity<ApiResponse<LeaveRequestDto.Response>> rejectLeave(
            @PathVariable UUID id,
            @RequestBody LeaveRequestDto.ApproveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.rejectLeave(id, request)));
    }

    @DeleteMapping("/leave/{id}")
    @PreAuthorize("hasAuthority('HR.CREATE')")
    @Operation(summary = "Cancel a leave request")
    public ResponseEntity<ApiResponse<Void>> cancelLeave(@PathVariable UUID id) {
        hrService.cancelLeave(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('HR.VIEW')")
    @Operation(summary = "HR dashboard — headcount, attendance and leave stats")
    public ResponseEntity<ApiResponse<HrDashboardResponse>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(hrService.getDashboard()));
    }
}
