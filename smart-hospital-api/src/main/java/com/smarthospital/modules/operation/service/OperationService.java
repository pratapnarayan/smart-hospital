package com.smarthospital.modules.operation.service;

import com.smarthospital.core.exception.ApiException;
import com.smarthospital.core.pagination.PageResponse;
import com.smarthospital.modules.hr.repository.EmployeeRepository;
import com.smarthospital.modules.inventory.domain.InventoryItem;
import com.smarthospital.modules.inventory.repository.InventoryItemRepository;
import com.smarthospital.modules.ipd.repository.IpdAdmissionRepository;
import com.smarthospital.modules.operation.domain.OperationTheatre;
import com.smarthospital.modules.operation.domain.OtConsumable;
import com.smarthospital.modules.operation.domain.OtSchedule;
import com.smarthospital.modules.operation.domain.OtSchedule.Status;
import com.smarthospital.modules.operation.dto.*;
import com.smarthospital.modules.operation.repository.OperationTheatreRepository;
import com.smarthospital.modules.operation.repository.OtScheduleRepository;
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
public class OperationService {

    private static final Logger log = LoggerFactory.getLogger(OperationService.class);

    private final OperationTheatreRepository  theatreRepository;
    private final OtScheduleRepository        scheduleRepository;
    private final IpdAdmissionRepository      admissionRepository;
    private final EmployeeRepository          employeeRepository;
    private final InventoryItemRepository     inventoryItemRepository;

    public OperationService(OperationTheatreRepository  theatreRepository,
                            OtScheduleRepository        scheduleRepository,
                            IpdAdmissionRepository      admissionRepository,
                            EmployeeRepository          employeeRepository,
                            InventoryItemRepository     inventoryItemRepository) {
        this.theatreRepository    = theatreRepository;
        this.scheduleRepository   = scheduleRepository;
        this.admissionRepository  = admissionRepository;
        this.employeeRepository   = employeeRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    // ── Theatres ──────────────────────────────────────────────────────────────

    public List<TheatreResponse> listTheatres() {
        return theatreRepository.findByActiveTrue().stream().map(TheatreResponse::from).toList();
    }

    @Transactional
    public TheatreResponse createTheatre(TheatreRequest req) {
        if (theatreRepository.existsByTheatreNumberIgnoreCase(req.theatreNumber()))
            throw ApiException.conflict("THEATRE_EXISTS",
                    "Theatre number '" + req.theatreNumber() + "' already in use");
        OperationTheatre theatre = OperationTheatre.builder()
                .theatreNumber(req.theatreNumber().toUpperCase())
                .name(req.name())
                .type(req.type())
                .build();
        return TheatreResponse.from(theatreRepository.save(theatre));
    }

    // ── Schedules ─────────────────────────────────────────────────────────────

    @Transactional
    public OtScheduleResponse scheduleOperation(OtScheduleCreateRequest req) {
        OperationTheatre theatre = theatreRepository.findById(req.theatreId())
                .orElseThrow(() -> ApiException.notFound("THEATRE_NOT_FOUND",
                        "OT " + req.theatreId() + " not found"));

        // Resolve patient from IPD admission if provided
        UUID   patientId   = req.patientId();
        String patientName = req.patientName();
        if (req.admissionId() != null) {
            var admission = admissionRepository.findById(req.admissionId())
                    .orElseThrow(() -> ApiException.notFound("ADMISSION_NOT_FOUND",
                            "IPD admission " + req.admissionId() + " not found"));
            patientId   = admission.getPatientId();
            patientName = admission.getPatientName();
        }
        if (patientName == null || patientName.isBlank())
            throw ApiException.badRequest("PATIENT_NAME_REQUIRED",
                    "Patient name is required when no admission ID is provided");

        // Resolve surgeon name from HR if surgeonId provided
        String surgeonName = req.surgeonName();
        if (req.surgeonId() != null) {
            var emp = employeeRepository.findById(req.surgeonId()).orElse(null);
            if (emp != null) surgeonName = emp.getFirstName() + " " + emp.getLastName();
        }

        // Resolve anesthetist name
        String anesthetistName = req.anesthetistName();
        if (req.anesthetistId() != null) {
            var emp = employeeRepository.findById(req.anesthetistId()).orElse(null);
            if (emp != null) anesthetistName = emp.getFirstName() + " " + emp.getLastName();
        }

        OtSchedule schedule = OtSchedule.builder()
                .scheduleNumber(generateScheduleNumber())
                .admissionId(req.admissionId())
                .patientId(patientId)
                .patientName(patientName)
                .theatreId(theatre.getId())
                .theatreName(theatre.getName())
                .scheduledDate(req.scheduledDate())
                .scheduledStart(req.scheduledStart())
                .estimatedDurationMins(req.estimatedDurationMins() > 0 ? req.estimatedDurationMins() : 60)
                .procedureName(req.procedureName())
                .operationType(req.operationType() != null ? req.operationType() : OtSchedule.OperationType.ELECTIVE)
                .priority(req.priority() != null ? req.priority() : OtSchedule.Priority.ROUTINE)
                .surgeonId(req.surgeonId())
                .surgeonName(surgeonName)
                .anesthetistId(req.anesthetistId())
                .anesthetistName(anesthetistName)
                .assistantNames(req.assistantNames())
                .preOpDiagnosis(req.preOpDiagnosis())
                .bloodRequestId(req.bloodRequestId())
                .bloodRequestNumber(req.bloodRequestNumber())
                .notes(req.notes())
                .build();

        OtSchedule saved = scheduleRepository.save(schedule);
        log.info("OT scheduled {} — {} in {} on {}",
                saved.getScheduleNumber(), saved.getProcedureName(),
                saved.getTheatreName(), saved.getScheduledDate());
        return OtScheduleResponse.from(saved);
    }

    public OtScheduleResponse getSchedule(UUID id) {
        return OtScheduleResponse.from(findOrThrow(id));
    }

    public PageResponse<OtScheduleResponse> listSchedules(
            LocalDate date, Status status, UUID theatreId, Pageable pageable) {
        if (theatreId != null && date != null)
            return PageResponse.of(scheduleRepository
                    .findByTheatreIdAndScheduledDate(theatreId, date, pageable)
                    .map(OtScheduleResponse::from));
        if (date != null && status != null)
            return PageResponse.of(scheduleRepository
                    .findByScheduledDateAndStatus(date, status, pageable)
                    .map(OtScheduleResponse::from));
        if (date != null)
            return PageResponse.of(scheduleRepository
                    .findByScheduledDate(date, pageable)
                    .map(OtScheduleResponse::from));
        if (status != null)
            return PageResponse.of(scheduleRepository.findByStatus(status, pageable)
                    .map(OtScheduleResponse::from));
        return PageResponse.of(scheduleRepository.findAll(pageable).map(OtScheduleResponse::from));
    }

    @Transactional
    public OtScheduleResponse startOperation(UUID id) {
        OtSchedule s = findOrThrow(id);
        if (s.getStatus() != Status.SCHEDULED)
            throw ApiException.badRequest("INVALID_STATUS", "Only SCHEDULED operations can be started");
        s.setStatus(Status.IN_PROGRESS);
        log.info("OT {} started", s.getScheduleNumber());
        return OtScheduleResponse.from(scheduleRepository.save(s));
    }

    @Transactional
    public OtScheduleResponse completeOperation(UUID id, CompleteOperationRequest req) {
        OtSchedule s = findOrThrow(id);
        if (s.getStatus() != Status.IN_PROGRESS && s.getStatus() != Status.SCHEDULED)
            throw ApiException.badRequest("INVALID_STATUS",
                    "Operation must be SCHEDULED or IN_PROGRESS to complete");
        if (req.actualEnd().isBefore(req.actualStart()))
            throw ApiException.badRequest("INVALID_TIME", "Actual end time must be after start time");

        s.setActualStart(req.actualStart());
        s.setActualEnd(req.actualEnd());
        s.setAnesthesiaType(req.anesthesiaType());
        s.setPostOpDiagnosis(req.postOpDiagnosis());
        s.setProcedureDetails(req.procedureDetails());
        s.setComplications(req.complications());
        s.setSurgeonNotes(req.surgeonNotes());
        s.setOutcome(req.outcome());
        s.setPatientConditionAfter(req.patientConditionAfter());
        s.setStatus(Status.COMPLETED);

        // Deduct consumables from inventory
        if (req.consumables() != null) {
            for (OtConsumableRequest cr : req.consumables()) {
                InventoryItem item = inventoryItemRepository.findById(cr.itemId())
                        .orElseThrow(() -> ApiException.notFound("ITEM_NOT_FOUND",
                                "Inventory item " + cr.itemId() + " not found"));
                if (item.getCurrentStock() < cr.quantityUsed())
                    throw ApiException.badRequest("INSUFFICIENT_STOCK",
                            "Item '" + item.getName() + "': available=" + item.getCurrentStock()
                            + ", requested=" + cr.quantityUsed());
                item.setCurrentStock(item.getCurrentStock() - cr.quantityUsed());
                inventoryItemRepository.save(item);
                s.getConsumables().add(new OtConsumable(s, item, cr.quantityUsed()));
            }
        }

        OtSchedule saved = scheduleRepository.save(s);
        log.info("OT {} completed — outcome={}, patient={}",
                saved.getScheduleNumber(), saved.getOutcome(), saved.getPatientConditionAfter());
        return OtScheduleResponse.from(saved);
    }

    @Transactional
    public OtScheduleResponse postponeOperation(UUID id, String notes) {
        OtSchedule s = findOrThrow(id);
        if (s.getStatus() != Status.SCHEDULED)
            throw ApiException.badRequest("INVALID_STATUS", "Only SCHEDULED operations can be postponed");
        s.setStatus(Status.POSTPONED);
        if (notes != null) s.setNotes(notes);
        return OtScheduleResponse.from(scheduleRepository.save(s));
    }

    @Transactional
    public OtScheduleResponse cancelOperation(UUID id, String notes) {
        OtSchedule s = findOrThrow(id);
        if (s.getStatus() == Status.COMPLETED)
            throw ApiException.badRequest("ALREADY_COMPLETED", "Cannot cancel a completed operation");
        s.setStatus(Status.CANCELLED);
        if (notes != null) s.setNotes(notes);
        return OtScheduleResponse.from(scheduleRepository.save(s));
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    public OtDashboardResponse getDashboard() {
        LocalDate today      = LocalDate.now();
        // Rolling 30-day window so the dashboard always shows data even on day 1 of a new month.
        LocalDate monthStart = today.minusDays(29);

        List<OtScheduleResponse> todayList = scheduleRepository
                .findByScheduledDateOrderByScheduledStartAsc(today)
                .stream().map(OtScheduleResponse::from).toList();

        List<OtDashboardResponse.TheatreUtilization> utilization =
                scheduleRepository.countByTheatreForPeriod(monthStart, today).stream()
                        .map(r -> new OtDashboardResponse.TheatreUtilization(
                                (String) r[0], (Long) r[1]))
                        .toList();

        return new OtDashboardResponse(
                scheduleRepository.countByStatusAndScheduledDate(Status.SCHEDULED, today),
                scheduleRepository.countByStatusAndScheduledDate(Status.IN_PROGRESS, today),
                scheduleRepository.countByStatusAndScheduledDate(Status.COMPLETED, today),
                scheduleRepository.countByScheduledDateBetween(monthStart, today),
                todayList,
                utilization
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OtSchedule findOrThrow(UUID id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SCHEDULE_NOT_FOUND",
                        "OT schedule " + id + " not found"));
    }

    private String generateScheduleNumber() {
        int year = LocalDate.now().getYear();
        long seq = scheduleRepository.nextSequenceForYear(year);
        return String.format("OTS-%d-%05d", year, seq);
    }
}
