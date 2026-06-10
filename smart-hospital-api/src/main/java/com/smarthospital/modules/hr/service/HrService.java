package com.smarthospital.modules.hr.service;

import com.smarthospital.core.exception.ApiException;
import com.smarthospital.core.pagination.PageResponse;
import com.smarthospital.modules.hr.domain.*;
import com.smarthospital.modules.hr.domain.AttendanceRecord.AttendanceStatus;
import com.smarthospital.modules.hr.domain.Employee.EmployeeStatus;
import com.smarthospital.modules.hr.domain.LeaveRequest.LeaveStatus;
import com.smarthospital.modules.hr.dto.*;
import com.smarthospital.modules.hr.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class HrService {

    private static final Logger log = LoggerFactory.getLogger(HrService.class);

    private final HrDepartmentRepository departmentRepository;
    private final DesignationRepository  designationRepository;
    private final EmployeeRepository     employeeRepository;
    private final AttendanceRepository   attendanceRepository;
    private final LeaveRequestRepository leaveRepository;

    public HrService(HrDepartmentRepository departmentRepository,
                     DesignationRepository  designationRepository,
                     EmployeeRepository     employeeRepository,
                     AttendanceRepository   attendanceRepository,
                     LeaveRequestRepository leaveRepository) {
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
        this.employeeRepository   = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRepository      = leaveRepository;
    }

    // ── Departments ───────────────────────────────────────────────────────────

    public List<DepartmentResponse> listDepartments() {
        return departmentRepository.findByActiveTrue().stream().map(DepartmentResponse::from).toList();
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest req) {
        if (departmentRepository.existsByNameIgnoreCase(req.name()))
            throw ApiException.conflict("DEPT_EXISTS", "Department '" + req.name() + "' already exists");
        if (departmentRepository.existsByCodeIgnoreCase(req.code()))
            throw ApiException.conflict("DEPT_CODE_EXISTS", "Code '" + req.code() + "' already in use");
        return DepartmentResponse.from(departmentRepository.save(
                HrDepartment.builder().name(req.name()).code(req.code().toUpperCase()).build()));
    }

    // ── Designations ──────────────────────────────────────────────────────────

    public List<DesignationResponse> listDesignations(UUID departmentId) {
        if (departmentId != null)
            return designationRepository.findByDepartmentIdAndActiveTrue(departmentId)
                    .stream().map(DesignationResponse::from).toList();
        return designationRepository.findByActiveTrue().stream().map(DesignationResponse::from).toList();
    }

    @Transactional
    public DesignationResponse createDesignation(DesignationRequest req) {
        return DesignationResponse.from(designationRepository.save(
                Designation.builder().title(req.title()).departmentId(req.departmentId()).build()));
    }

    // ── Employees ─────────────────────────────────────────────────────────────

    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest req) {
        if (req.email() != null && employeeRepository.existsByEmailIgnoreCase(req.email()))
            throw ApiException.conflict("EMAIL_EXISTS", "Email '" + req.email() + "' already registered");

        String code = generateEmployeeCode();
        Employee emp = Employee.builder()
                .employeeCode(code)
                .firstName(req.firstName())
                .lastName(req.lastName())
                .joinDate(req.joinDate())
                .dateOfBirth(req.dateOfBirth())
                .gender(req.gender())
                .mobile(req.mobile())
                .email(req.email())
                .address(req.address())
                .bloodGroup(req.bloodGroup())
                .departmentId(req.departmentId())
                .designationId(req.designationId())
                .userId(req.userId())
                .employmentType(req.employmentType() != null
                        ? req.employmentType() : Employee.EmploymentType.FULL_TIME)
                .build();

        Employee saved = employeeRepository.save(emp);
        log.info("Employee {} created: {} {}", code, req.firstName(), req.lastName());
        return EmployeeResponse.from(saved);
    }

    public EmployeeResponse getEmployee(UUID id) {
        return EmployeeResponse.from(findEmployeeOrThrow(id));
    }

    public PageResponse<EmployeeResponse> listEmployees(UUID departmentId, Pageable pageable) {
        if (departmentId != null)
            return PageResponse.of(employeeRepository.findByDepartmentId(departmentId, pageable)
                    .map(EmployeeResponse::from));
        return PageResponse.of(employeeRepository.findAll(pageable).map(EmployeeResponse::from));
    }

    public PageResponse<EmployeeResponse> searchEmployees(String query, Pageable pageable) {
        return PageResponse.of(employeeRepository.search(query, pageable).map(EmployeeResponse::from));
    }

    @Transactional
    public EmployeeResponse updateEmployee(UUID id, EmployeeUpdateRequest req) {
        Employee emp = findEmployeeOrThrow(id);

        if (req.firstName()      != null) emp.setFirstName(req.firstName());
        if (req.lastName()       != null) emp.setLastName(req.lastName());
        if (req.dateOfBirth()    != null) emp.setDateOfBirth(req.dateOfBirth());
        if (req.gender()         != null) emp.setGender(req.gender());
        if (req.mobile()         != null) emp.setMobile(req.mobile());
        if (req.email()          != null) emp.setEmail(req.email());
        if (req.address()        != null) emp.setAddress(req.address());
        if (req.bloodGroup()     != null) emp.setBloodGroup(req.bloodGroup());
        if (req.departmentId()   != null) emp.setDepartmentId(req.departmentId());
        if (req.designationId()  != null) emp.setDesignationId(req.designationId());
        if (req.userId()         != null) emp.setUserId(req.userId());
        if (req.employmentType() != null) emp.setEmploymentType(req.employmentType());
        if (req.status()         != null) emp.setStatus(req.status());

        return EmployeeResponse.from(employeeRepository.save(emp));
    }

    @Transactional
    public EmployeeResponse updateEmployeePhoto(UUID id, String photoUrl) {
        Employee emp = findEmployeeOrThrow(id);
        emp.setProfilePhoto(photoUrl);
        return EmployeeResponse.from(employeeRepository.save(emp));
    }

    @Transactional
    public void deleteEmployee(UUID id) {
        findEmployeeOrThrow(id);
        employeeRepository.deleteById(id);
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    @Transactional
    public AttendanceResponse markAttendance(AttendanceRequest req) {
        findEmployeeOrThrow(req.employeeId());

        AttendanceRecord record = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(req.employeeId(), req.attendanceDate())
                .map(existing -> {
                    existing.setStatus(req.status());
                    existing.setCheckIn(req.checkIn());
                    existing.setCheckOut(req.checkOut());
                    existing.setNotes(req.notes());
                    return existing;
                })
                .orElseGet(() -> AttendanceRecord.builder()
                        .employeeId(req.employeeId())
                        .attendanceDate(req.attendanceDate())
                        .checkIn(req.checkIn())
                        .checkOut(req.checkOut())
                        .status(req.status())
                        .notes(req.notes())
                        .build());

        return AttendanceResponse.from(attendanceRepository.save(record));
    }

    public List<AttendanceResponse> getAttendanceByDate(LocalDate date) {
        return attendanceRepository
                .findByAttendanceDateOrderByCreatedAtAsc(date != null ? date : LocalDate.now())
                .stream().map(AttendanceResponse::from).toList();
    }

    public List<AttendanceResponse> getEmployeeAttendance(UUID employeeId) {
        findEmployeeOrThrow(employeeId);
        return attendanceRepository.findByEmployeeIdOrderByAttendanceDateDesc(employeeId)
                .stream().map(AttendanceResponse::from).toList();
    }

    // ── Leave requests ────────────────────────────────────────────────────────

    @Transactional
    public LeaveRequestDto.Response applyLeave(LeaveRequestDto.CreateRequest req) {
        Employee emp = findEmployeeOrThrow(req.employeeId());
        if (req.toDate().isBefore(req.fromDate()))
            throw ApiException.badRequest("INVALID_DATES", "To-date must be on or after from-date");

        long days = req.fromDate().datesUntil(req.toDate().plusDays(1)).count();
        String leaveNumber = generateLeaveNumber();

        LeaveRequest leave = LeaveRequest.builder()
                .leaveNumber(leaveNumber)
                .employeeId(emp.getId())
                .employeeName(emp.getFirstName() + " " + emp.getLastName())
                .leaveType(req.leaveType())
                .fromDate(req.fromDate())
                .toDate(req.toDate())
                .totalDays((int) days)
                .reason(req.reason())
                .build();

        return LeaveRequestDto.Response.from(leaveRepository.save(leave));
    }

    public PageResponse<LeaveRequestDto.Response> listLeaves(UUID employeeId,
                                                              LeaveStatus status,
                                                              Pageable pageable) {
        if (employeeId != null)
            return PageResponse.of(leaveRepository.findByEmployeeId(employeeId, pageable)
                    .map(LeaveRequestDto.Response::from));
        if (status != null)
            return PageResponse.of(leaveRepository.findByStatus(status, pageable)
                    .map(LeaveRequestDto.Response::from));
        return PageResponse.of(leaveRepository.findAll(pageable).map(LeaveRequestDto.Response::from));
    }

    @Transactional
    public LeaveRequestDto.Response approveLeave(UUID id, LeaveRequestDto.ApproveRequest req) {
        LeaveRequest leave = findLeaveOrThrow(id);
        if (leave.getStatus() != LeaveStatus.PENDING)
            throw ApiException.badRequest("LEAVE_NOT_PENDING", "Only PENDING leaves can be approved");
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApproverNotes(req.approverNotes());
        return LeaveRequestDto.Response.from(leaveRepository.save(leave));
    }

    @Transactional
    public LeaveRequestDto.Response rejectLeave(UUID id, LeaveRequestDto.ApproveRequest req) {
        LeaveRequest leave = findLeaveOrThrow(id);
        if (leave.getStatus() != LeaveStatus.PENDING)
            throw ApiException.badRequest("LEAVE_NOT_PENDING", "Only PENDING leaves can be rejected");
        leave.setStatus(LeaveStatus.REJECTED);
        leave.setApproverNotes(req.approverNotes());
        return LeaveRequestDto.Response.from(leaveRepository.save(leave));
    }

    @Transactional
    public void cancelLeave(UUID id) {
        LeaveRequest leave = findLeaveOrThrow(id);
        if (leave.getStatus() == LeaveStatus.APPROVED || leave.getStatus() == LeaveStatus.REJECTED)
            throw ApiException.badRequest("LEAVE_FINALIZED", "Cannot cancel an already " + leave.getStatus() + " leave");
        leave.setStatus(LeaveStatus.CANCELLED);
        leaveRepository.save(leave);
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    public HrDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        long total    = employeeRepository.count();
        long active   = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long present  = attendanceRepository.countByAttendanceDateAndStatus(today, AttendanceStatus.PRESENT);
        long absent   = attendanceRepository.countByAttendanceDateAndStatus(today, AttendanceStatus.ABSENT);
        long onLeave  = leaveRepository.countOnLeaveOnDate(today);
        long pending  = leaveRepository.countByStatus(LeaveStatus.PENDING);
        return new HrDashboardResponse(total, active, present, absent, onLeave, pending);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Employee findEmployeeOrThrow(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("EMPLOYEE_NOT_FOUND", "Employee " + id + " not found"));
    }

    private LeaveRequest findLeaveOrThrow(UUID id) {
        return leaveRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("LEAVE_NOT_FOUND", "Leave request " + id + " not found"));
    }

    private String generateEmployeeCode() {
        int year = LocalDate.now().getYear();
        long seq = employeeRepository.nextSequenceForYear(year);
        return String.format("EMP-%d-%04d", year, seq);
    }

    private String generateLeaveNumber() {
        int year = LocalDate.now().getYear();
        long seq = leaveRepository.nextSequenceForYear(year);
        return String.format("LV-%d-%04d", year, seq);
    }
}
