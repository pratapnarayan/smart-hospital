package com.smarthospital.modules.doctor.service;

import com.smarthospital.core.exception.ApiException;
import com.smarthospital.core.pagination.PageResponse;
import com.smarthospital.modules.doctor.domain.DoctorProfile;
import com.smarthospital.modules.doctor.domain.DoctorSchedule;
import com.smarthospital.modules.doctor.domain.Specialization;
import com.smarthospital.modules.doctor.dto.*;
import com.smarthospital.modules.doctor.repository.DoctorProfileRepository;
import com.smarthospital.modules.doctor.repository.DoctorScheduleRepository;
import com.smarthospital.modules.doctor.repository.SpecializationRepository;
import com.smarthospital.modules.frontoffice.repository.AppointmentRepository;
import com.smarthospital.modules.hr.domain.Employee;
import com.smarthospital.modules.hr.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);

    private final DoctorProfileRepository  doctorRepo;
    private final SpecializationRepository specRepo;
    private final DoctorScheduleRepository scheduleRepo;
    private final EmployeeRepository       employeeRepo;
    private final AppointmentRepository    appointmentRepo;

    public DoctorService(DoctorProfileRepository  doctorRepo,
                         SpecializationRepository specRepo,
                         DoctorScheduleRepository scheduleRepo,
                         EmployeeRepository       employeeRepo,
                         AppointmentRepository    appointmentRepo) {
        this.doctorRepo      = doctorRepo;
        this.specRepo        = specRepo;
        this.scheduleRepo    = scheduleRepo;
        this.employeeRepo    = employeeRepo;
        this.appointmentRepo = appointmentRepo;
    }

    // ── Specializations ──────────────────────────────────────────────────────

    public List<SpecializationResponse> listSpecializations() {
        return specRepo.findAllByActiveTrue().stream().map(SpecializationResponse::from).toList();
    }

    @Transactional
    public SpecializationResponse createSpecialization(SpecializationRequest req) {
        if (specRepo.existsByNameIgnoreCase(req.name())) {
            throw ApiException.conflict("SPEC_EXISTS", "Specialization already exists: " + req.name());
        }
        Specialization s = Specialization.builder()
            .name(req.name()).code(req.code().toUpperCase()).description(req.description()).build();
        return SpecializationResponse.from(specRepo.save(s));
    }

    @Transactional
    public SpecializationResponse updateSpecialization(UUID id, SpecializationRequest req) {
        Specialization s = specRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("SPEC_NOT_FOUND", "Specialization not found"));
        if (!s.getName().equalsIgnoreCase(req.name()) && specRepo.existsByNameIgnoreCase(req.name())) {
            throw ApiException.conflict("SPEC_EXISTS", "Specialization already exists: " + req.name());
        }
        s.setName(req.name());
        s.setCode(req.code().toUpperCase());
        s.setDescription(req.description());
        return SpecializationResponse.from(specRepo.save(s));
    }

    // ── Doctor Profiles ──────────────────────────────────────────────────────

    public PageResponse<DoctorProfileResponse> listDoctors(String search, UUID deptId, UUID specId, Pageable pageable) {
        Page<DoctorProfile> page = doctorRepo.search(search, deptId, specId, pageable);
        return PageResponse.of(page.map(p -> {
            Employee e = employeeRepo.findById(p.getEmployeeId())
                .orElseThrow(() -> ApiException.notFound("EMP_NOT_FOUND", "Employee not found"));
            return DoctorProfileResponse.from(p, e);
        }));
    }

    public DoctorProfileResponse getDoctor(UUID id) {
        DoctorProfile p = doctorRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("DOCTOR_NOT_FOUND", "Doctor not found"));
        Employee e = employeeRepo.findById(p.getEmployeeId())
            .orElseThrow(() -> ApiException.notFound("EMP_NOT_FOUND", "Employee not found"));
        return DoctorProfileResponse.from(p, e);
    }

    public DoctorProfileResponse getDoctorByEmployeeId(UUID employeeId) {
        DoctorProfile p = doctorRepo.findByEmployeeId(employeeId)
            .orElseThrow(() -> ApiException.notFound("DOCTOR_NOT_FOUND", "Doctor profile not found for employee"));
        Employee e = employeeRepo.findById(employeeId)
            .orElseThrow(() -> ApiException.notFound("EMP_NOT_FOUND", "Employee not found"));
        return DoctorProfileResponse.from(p, e);
    }

    @Transactional
    public DoctorProfileResponse createDoctorProfile(DoctorProfileRequest req) {
        Employee emp = employeeRepo.findById(req.employeeId())
            .orElseThrow(() -> ApiException.notFound("EMP_NOT_FOUND", "Employee not found: " + req.employeeId()));
        if (doctorRepo.existsByEmployeeId(req.employeeId())) {
            throw ApiException.conflict("DOCTOR_EXISTS", "Doctor profile already exists for this employee");
        }
        DoctorProfile p = DoctorProfile.builder()
            .employeeId(req.employeeId())
            .profilePhoto(req.profilePhoto())
            .biography(req.biography())
            .qualifications(req.qualifications())
            .experienceYears(req.experienceYears() != null ? req.experienceYears() : 0)
            .consultationFee(req.consultationFee() != null ? req.consultationFee() : java.math.BigDecimal.ZERO)
            .followUpFee(req.followUpFee() != null ? req.followUpFee() : java.math.BigDecimal.ZERO)
            .teleConsultationFee(req.teleConsultationFee() != null ? req.teleConsultationFee() : java.math.BigDecimal.ZERO)
            .languages(req.languages())
            .onlineBookingEnabled(req.onlineBookingEnabled() != null ? req.onlineBookingEnabled() : true)
            .displayOnPortal(req.displayOnPortal() != null ? req.displayOnPortal() : true)
            .build();
        if (req.specializationIds() != null && !req.specializationIds().isEmpty()) {
            List<Specialization> found = new ArrayList<>(specRepo.findAllById(req.specializationIds()));
            if (found.size() != req.specializationIds().size()) {
                Set<UUID> foundIds = found.stream().map(Specialization::getId).collect(Collectors.toSet());
                List<UUID> missing = req.specializationIds().stream()
                    .filter(sid -> !foundIds.contains(sid)).toList();
                throw ApiException.badRequest("SPEC_NOT_FOUND", "Unknown specialization IDs: " + missing);
            }
            p.setSpecializations(new HashSet<>(found));
        }
        DoctorProfile saved = doctorRepo.save(p);
        log.info("Doctor profile created for employee {}", emp.getEmployeeCode());
        return DoctorProfileResponse.from(saved, emp);
    }

    @Transactional
    public DoctorProfileResponse updateDoctorProfile(UUID id, DoctorProfileRequest req) {
        DoctorProfile p = doctorRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("DOCTOR_NOT_FOUND", "Doctor not found"));
        Employee emp = employeeRepo.findById(p.getEmployeeId())
            .orElseThrow(() -> ApiException.notFound("EMP_NOT_FOUND", "Employee not found"));
        if (req.profilePhoto()        != null) p.setProfilePhoto(req.profilePhoto());
        if (req.biography()           != null) p.setBiography(req.biography());
        if (req.qualifications()      != null) p.setQualifications(req.qualifications());
        if (req.experienceYears()     != null) p.setExperienceYears(req.experienceYears());
        if (req.consultationFee()     != null) p.setConsultationFee(req.consultationFee());
        if (req.followUpFee()         != null) p.setFollowUpFee(req.followUpFee());
        if (req.teleConsultationFee() != null) p.setTeleConsultationFee(req.teleConsultationFee());
        if (req.languages()           != null) p.setLanguages(req.languages());
        if (req.awards()              != null) p.setAwards(req.awards());
        if (req.achievements()        != null) p.setAchievements(req.achievements());
        if (req.publications()        != null) p.setPublications(req.publications());
        if (req.onlineBookingEnabled()!= null) p.setOnlineBookingEnabled(req.onlineBookingEnabled());
        if (req.displayOnPortal()     != null) p.setDisplayOnPortal(req.displayOnPortal());
        if (req.specializationIds() != null) {
            if (!req.specializationIds().isEmpty()) {
                List<Specialization> found = new ArrayList<>(specRepo.findAllById(req.specializationIds()));
                if (found.size() != req.specializationIds().size()) {
                    Set<UUID> foundIds = found.stream().map(Specialization::getId).collect(Collectors.toSet());
                    List<UUID> missing = req.specializationIds().stream()
                        .filter(sid -> !foundIds.contains(sid)).toList();
                    throw ApiException.badRequest("SPEC_NOT_FOUND", "Unknown specialization IDs: " + missing);
                }
                p.setSpecializations(new HashSet<>(found));
            } else {
                p.setSpecializations(new HashSet<>());
            }
        }
        return DoctorProfileResponse.from(doctorRepo.save(p), emp);
    }

    @Transactional
    public DoctorProfileResponse updateDoctorPhoto(UUID id, String photoUrl) {
        DoctorProfile p = doctorRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("DOCTOR_NOT_FOUND", "Doctor not found"));
        p.setProfilePhoto(photoUrl);
        Employee emp = employeeRepo.findById(p.getEmployeeId())
            .orElseThrow(() -> ApiException.notFound("EMP_NOT_FOUND", "Employee not found"));
        return DoctorProfileResponse.from(doctorRepo.save(p), emp);
    }

    // ── Schedules ────────────────────────────────────────────────────────────

    public List<DoctorScheduleResponse> getSchedules(UUID doctorId) {
        return scheduleRepo.findByDoctorIdAndActiveTrue(doctorId).stream()
            .map(DoctorScheduleResponse::from).toList();
    }

    @Transactional
    public List<DoctorScheduleResponse> saveSchedules(UUID doctorId, List<DoctorScheduleRequest> requests) {
        doctorRepo.findById(doctorId)
            .orElseThrow(() -> ApiException.notFound("DOCTOR_NOT_FOUND", "Doctor not found"));
        scheduleRepo.deleteByDoctorId(doctorId);
        List<DoctorSchedule> schedules = requests.stream().map(req ->
            DoctorSchedule.builder()
                .doctorId(doctorId)
                .dayOfWeek(req.dayOfWeek())
                .shiftStart(req.shiftStart())
                .shiftEnd(req.shiftEnd())
                .slotDurationMins(req.slotDurationMins() > 0 ? req.slotDurationMins() : 15)
                .active(true)
                .build()
        ).toList();
        return scheduleRepo.saveAll(schedules).stream().map(DoctorScheduleResponse::from).toList();
    }

    // ── Available Slots ──────────────────────────────────────────────────────

    public List<AvailableSlotResponse> getAvailableSlots(UUID doctorId, LocalDate from, LocalDate to) {
        List<DoctorSchedule> schedules = scheduleRepo.findByDoctorIdAndActiveTrue(doctorId);
        List<AvailableSlotResponse> result = new ArrayList<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            final LocalDate date = current;
            String dow = date.getDayOfWeek().name();
            List<String> daySlots = new ArrayList<>();
            for (DoctorSchedule sched : schedules) {
                if (!sched.getDayOfWeek().name().equals(dow)) continue;
                List<String> bookedSlots = appointmentRepo.findByDoctorIdAndDate(doctorId, date).stream()
                    .map(a -> a.getTimeSlot()).filter(Objects::nonNull).toList();
                LocalTime t = sched.getShiftStart();
                while (!t.isAfter(sched.getShiftEnd().minusMinutes(sched.getSlotDurationMins()))) {
                    String slot = t.toString();
                    if (!bookedSlots.contains(slot)) daySlots.add(slot);
                    t = t.plusMinutes(sched.getSlotDurationMins());
                }
            }
            if (!daySlots.isEmpty()) result.add(new AvailableSlotResponse(date, daySlots));
            current = current.plusDays(1);
        }
        return result;
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    public DoctorDashboardResponse getDashboard() {
        long total  = doctorRepo.count();
        long active = doctorRepo.search(null, null, null, PageRequest.of(0, 1)).getTotalElements();
        String today = LocalDate.now().getDayOfWeek().name();
        long availableToday = doctorRepo.findAll().stream()
            .filter(p -> scheduleRepo.findByDoctorIdAndActiveTrue(p.getId()).stream()
                .anyMatch(s -> s.getDayOfWeek().name().equals(today)))
            .count();
        long totalSpecs = specRepo.count();
        return new DoctorDashboardResponse(total, active, availableToday, totalSpecs);
    }
}
