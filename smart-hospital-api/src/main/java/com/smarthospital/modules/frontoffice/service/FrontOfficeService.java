package com.smarthospital.modules.frontoffice.service;

import com.smarthospital.core.exception.ApiException;
import com.smarthospital.core.pagination.PageResponse;
import com.smarthospital.modules.frontoffice.domain.Appointment;
import com.smarthospital.modules.frontoffice.domain.Appointment.AppointmentStatus;
import com.smarthospital.modules.frontoffice.domain.OpdToken;
import com.smarthospital.modules.frontoffice.domain.OpdToken.TokenStatus;
import com.smarthospital.modules.frontoffice.dto.*;
import com.smarthospital.modules.frontoffice.repository.AppointmentRepository;
import com.smarthospital.modules.frontoffice.repository.OpdTokenRepository;
import com.smarthospital.modules.patient.domain.Patient;
import com.smarthospital.modules.patient.repository.PatientRepository;
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
public class FrontOfficeService {

    private static final Logger log = LoggerFactory.getLogger(FrontOfficeService.class);

    private final AppointmentRepository appointmentRepository;
    private final OpdTokenRepository    tokenRepository;
    private final PatientRepository     patientRepository;

    public FrontOfficeService(AppointmentRepository appointmentRepository,
                              OpdTokenRepository    tokenRepository,
                              PatientRepository     patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.tokenRepository       = tokenRepository;
        this.patientRepository     = patientRepository;
    }

    // ── Appointments ─────────────────────────────────────────────────────────

    @Transactional
    public AppointmentResponse createAppointment(AppointmentCreateRequest req) {
        Patient patient = patientRepository.findById(req.patientId())
                .orElseThrow(() -> ApiException.notFound("PATIENT_NOT_FOUND",
                        "Patient " + req.patientId() + " not found"));

        LocalDate date = req.appointmentDate() != null ? req.appointmentDate() : LocalDate.now();

        Appointment appointment = Appointment.builder()
                .appointmentNumber(generateAppointmentNumber(date))
                .patientId(patient.getId())
                .patientName(patient.getFirstName() + " " + patient.getLastName())
                .patientMobile(patient.getMobile())
                .doctorId(req.doctorId())
                .doctorName(req.doctorName())
                .department(req.department())
                .appointmentDate(date)
                .timeSlot(req.timeSlot())
                .appointmentType(req.appointmentType() != null
                        ? req.appointmentType()
                        : Appointment.AppointmentType.CONSULTATION)
                .notes(req.notes())
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment {} created for patient {}", saved.getAppointmentNumber(), patient.getId());
        return AppointmentResponse.from(saved);
    }

    public AppointmentResponse getAppointment(UUID id) {
        return AppointmentResponse.from(findAppointmentOrThrow(id));
    }

    public PageResponse<AppointmentResponse> listByDate(LocalDate date, Pageable pageable) {
        LocalDate target = date != null ? date : LocalDate.now();
        return PageResponse.of(appointmentRepository.findByAppointmentDate(target, pageable)
                .map(AppointmentResponse::from));
    }

    public PageResponse<AppointmentResponse> listByPatient(UUID patientId, Pageable pageable) {
        return PageResponse.of(appointmentRepository.findByPatientId(patientId, pageable)
                .map(AppointmentResponse::from));
    }

    public PageResponse<AppointmentResponse> listUpcoming(Pageable pageable) {
        List<AppointmentStatus> active = List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED);
        return PageResponse.of(appointmentRepository
                .findUpcoming(LocalDate.now(), active, pageable)
                .map(AppointmentResponse::from));
    }

    public List<AppointmentResponse> listUpcomingByPatient(UUID patientId) {
        List<AppointmentStatus> active = List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED);
        return appointmentRepository
                .findUpcomingByPatient(patientId, LocalDate.now(), active)
                .stream().map(AppointmentResponse::from).toList();
    }

    public PageResponse<AppointmentResponse> listByDoctor(UUID doctorId, LocalDate date, Pageable pageable) {
        LocalDate target = date != null ? date : LocalDate.now();
        return PageResponse.of(
                appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, target, pageable)
                        .map(AppointmentResponse::from));
    }

    @Transactional
    public AppointmentResponse updateAppointment(UUID id, AppointmentUpdateRequest req) {
        Appointment apt = findAppointmentOrThrow(id);

        if (apt.getStatus() == AppointmentStatus.COMPLETED
                || apt.getStatus() == AppointmentStatus.CANCELLED) {
            throw ApiException.badRequest("APPOINTMENT_CLOSED",
                    "Cannot modify a " + apt.getStatus() + " appointment");
        }

        if (req.doctorId()         != null) apt.setDoctorId(req.doctorId());
        if (req.doctorName()       != null) apt.setDoctorName(req.doctorName());
        if (req.department()       != null) apt.setDepartment(req.department());
        if (req.appointmentDate()  != null) apt.setAppointmentDate(req.appointmentDate());
        if (req.timeSlot()         != null) apt.setTimeSlot(req.timeSlot());
        if (req.appointmentType()  != null) apt.setAppointmentType(req.appointmentType());
        if (req.status()           != null) apt.setStatus(req.status());
        if (req.notes()            != null) apt.setNotes(req.notes());

        return AppointmentResponse.from(appointmentRepository.save(apt));
    }

    @Transactional
    public void cancelAppointment(UUID id) {
        Appointment apt = findAppointmentOrThrow(id);
        if (apt.getStatus() == AppointmentStatus.COMPLETED) {
            throw ApiException.badRequest("APPOINTMENT_COMPLETED", "Cannot cancel a completed appointment");
        }
        apt.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(apt);
    }

    // ── OPD Tokens ────────────────────────────────────────────────────────────

    @Transactional
    public OpdTokenResponse generateToken(OpdTokenCreateRequest req) {
        Patient patient = patientRepository.findById(req.patientId())
                .orElseThrow(() -> ApiException.notFound("PATIENT_NOT_FOUND",
                        "Patient " + req.patientId() + " not found"));

        LocalDate today = LocalDate.now();
        long seq = tokenRepository.nextTokenNumber(today, req.department());
        String tokenNumber = String.format("T-%03d", seq);

        OpdToken token = OpdToken.builder()
                .tokenNumber(tokenNumber)
                .patientId(patient.getId())
                .patientName(patient.getFirstName() + " " + patient.getLastName())
                .patientMobile(patient.getMobile())
                .department(req.department())
                .doctorId(req.doctorId())
                .doctorName(req.doctorName())
                .tokenDate(today)
                .priority(req.priority() != null ? req.priority() : OpdToken.TokenPriority.NORMAL)
                .linkedAppointmentId(req.linkedAppointmentId())
                .build();

        OpdToken saved = tokenRepository.save(token);
        log.info("Token {} issued to patient {} for {}", tokenNumber, patient.getId(), req.department());
        return OpdTokenResponse.from(saved);
    }

    public List<OpdTokenResponse> listTokens(LocalDate date, String department) {
        LocalDate target = date != null ? date : LocalDate.now();
        if (department != null && !department.isBlank()) {
            return tokenRepository
                    .findByTokenDateAndDepartmentOrderByTokenNumberAsc(target, department)
                    .stream().map(OpdTokenResponse::from).toList();
        }
        return tokenRepository.findByTokenDateOrderByDepartmentAscTokenNumberAsc(target)
                .stream().map(OpdTokenResponse::from).toList();
    }

    public OpdTokenResponse getToken(UUID id) {
        return OpdTokenResponse.from(findTokenOrThrow(id));
    }

    @Transactional
    public OpdTokenResponse updateTokenStatus(UUID id, TokenStatus status) {
        OpdToken token = findTokenOrThrow(id);
        token.setStatus(status);
        return OpdTokenResponse.from(tokenRepository.save(token));
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    public FrontOfficeDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();

        long todayApts     = appointmentRepository.countByAppointmentDateAndStatus(today, AppointmentStatus.SCHEDULED)
                           + appointmentRepository.countByAppointmentDateAndStatus(today, AppointmentStatus.CONFIRMED)
                           + appointmentRepository.countByAppointmentDateAndStatus(today, AppointmentStatus.CHECKED_IN);
        long confirmed     = appointmentRepository.countByAppointmentDateAndStatus(today, AppointmentStatus.CONFIRMED);
        long checkedIn     = appointmentRepository.countByAppointmentDateAndStatus(today, AppointmentStatus.CHECKED_IN);

        long todayTokens   = tokenRepository.findByTokenDateOrderByDepartmentAscTokenNumberAsc(today).size();
        long waiting       = tokenRepository.findByTokenDateOrderByDepartmentAscTokenNumberAsc(today)
                .stream().filter(t -> t.getStatus() == TokenStatus.WAITING).count();
        long inProgress    = tokenRepository.findByTokenDateOrderByDepartmentAscTokenNumberAsc(today)
                .stream().filter(t -> t.getStatus() == TokenStatus.IN_PROGRESS).count();
        long completed     = tokenRepository.findByTokenDateOrderByDepartmentAscTokenNumberAsc(today)
                .stream().filter(t -> t.getStatus() == TokenStatus.COMPLETED).count();

        return new FrontOfficeDashboardResponse(
                todayApts, confirmed, checkedIn,
                todayTokens, waiting, inProgress, completed);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Appointment findAppointmentOrThrow(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("APPOINTMENT_NOT_FOUND",
                        "Appointment " + id + " not found"));
    }

    private OpdToken findTokenOrThrow(UUID id) {
        return tokenRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("TOKEN_NOT_FOUND",
                        "Token " + id + " not found"));
    }

    private String generateAppointmentNumber(LocalDate date) {
        long seq = appointmentRepository.nextSequenceForYear(date.getYear());
        return String.format("APT-%d-%05d", date.getYear(), seq);
    }
}
