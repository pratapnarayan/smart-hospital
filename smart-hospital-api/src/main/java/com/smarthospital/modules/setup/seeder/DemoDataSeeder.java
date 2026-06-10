package com.smarthospital.modules.setup.seeder;

import com.smarthospital.core.tenant.TenantContext;
import com.smarthospital.modules.bloodbank.domain.*;
import com.smarthospital.modules.bloodbank.repository.*;
import com.smarthospital.modules.doctor.domain.DoctorProfile;
import com.smarthospital.modules.doctor.domain.Specialization;
import com.smarthospital.modules.doctor.repository.DoctorProfileRepository;
import com.smarthospital.modules.doctor.repository.SpecializationRepository;
import com.smarthospital.modules.finance.domain.*;
import com.smarthospital.modules.finance.repository.*;
import com.smarthospital.modules.frontoffice.domain.Appointment;
import com.smarthospital.modules.frontoffice.repository.AppointmentRepository;
import com.smarthospital.modules.hr.domain.*;
import com.smarthospital.modules.hr.repository.*;
import com.smarthospital.modules.inventory.domain.*;
import com.smarthospital.modules.inventory.repository.*;
import com.smarthospital.modules.ipd.domain.*;
import com.smarthospital.modules.ipd.repository.*;
import com.smarthospital.modules.operation.domain.*;
import com.smarthospital.modules.operation.repository.*;
import com.smarthospital.modules.pathology.domain.*;
import com.smarthospital.modules.pathology.repository.*;
import com.smarthospital.modules.patient.domain.Patient;
import com.smarthospital.modules.patient.repository.PatientRepository;
import com.smarthospital.modules.pharmacy.domain.*;
import com.smarthospital.modules.pharmacy.repository.*;
import com.smarthospital.modules.opd.domain.OpdVisit;
import com.smarthospital.modules.opd.repository.OpdVisitRepository;
import com.smarthospital.modules.radiology.domain.*;
import com.smarthospital.modules.radiology.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Seeds ~510 demo records across all SmartHospital modules.
 *
 * Activated only with the "dev-demo" Spring profile.
 * On every run it WIPES the previous demo data first (hard-delete via JDBC,
 * bypassing Hibernate's soft-delete), then creates a fresh 6-month dataset
 * anchored to today — so re-running one month later gives dates that are one
 * month later, keeping dashboards and timelines current.
 *
 * Run command:
 *   --spring.profiles.active=dev,dev-demo
 */
@Component
@Profile("dev-demo")
@Order(3)           // after TenantMigrationService (1) and DevDataSeeder (2)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log    = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String TENANT = "hospital_001";
    private static final Random RNG    = new Random(42);   // fixed seed → reproducible structure

    // ── Static data pools ────────────────────────────────────────────────────

    private static final String[] M_FIRST = {
        "Rahul","Arjun","Vikram","Ravi","Suresh","Amit","Rajesh","Nitin","Sanjay","Manish",
        "Arun","Deepak","Pranav","Sandeep","Vaibhav","Kiran","Ajay","Vijay","Sachin","Rohit",
        "Pankaj","Gaurav","Vivek","Mahesh","Ramesh","Shyam","Tarun","Hemant","Naresh","Yogesh"
    };
    private static final String[] F_FIRST = {
        "Priya","Sunita","Anita","Kavya","Deepa","Asha","Meera","Nisha","Pooja","Rashmi",
        "Sonal","Shweta","Divya","Neha","Ritu","Smita","Gauri","Archana","Vandana","Aarti",
        "Swati","Rekha","Seema","Usha","Madhuri","Jyoti","Kavita","Lalita","Sushma","Geeta"
    };
    private static final String[] LAST = {
        "Sharma","Patel","Singh","Nair","Reddy","Iyer","Gupta","Joshi","Kumar","Mishra",
        "Verma","Desai","Rao","Mehta","Malhotra","Bose","Shah","Trivedi","Chavan","Pillai",
        "Kaur","Dubey","Saxena","Srivastava","Pandey","Yadav","Tiwari","Chauhan","Rajput","Thakur"
    };
    private static final String[] CITIES = {
        "Mumbai","Delhi","Pune","Bangalore","Chennai",
        "Hyderabad","Ahmedabad","Kolkata","Jaipur","Lucknow","Surat","Nagpur"
    };
    private static final String[] DIAGNOSES = {
        "Viral fever","Hypertension","Type 2 Diabetes Mellitus","Acute URTI",
        "Malaria (P. vivax)","Dengue fever","Gastroenteritis","Migraine",
        "Hypothyroidism","Bronchial Asthma","Iron Deficiency Anaemia","UTI",
        "Lumbar Spondylosis","Fracture neck of femur","Acute Appendicitis",
        "Inguinal Hernia","Tonsillitis","Knee Osteoarthritis","GERD",
        "Pneumonia","Cholelithiasis","DVT","Cellulitis","Pleural Effusion",
        "Chronic Kidney Disease","Peptic Ulcer Disease","Coronary Artery Disease"
    };
    private static final String[] SYMPTOMS = {
        "Fever and chills for 3 days","Headache and nausea","Chest pain and dyspnea",
        "Abdominal pain","Vomiting and loose stools","Joint pain and swelling",
        "Fatigue and weakness","Productive cough","Lower back pain radiating to leg",
        "Dizziness and tinnitus","Burning micturition","Shortness of breath on exertion",
        "Palpitations and sweating","Loss of appetite and weight loss"
    };
    private static final String[] BLOOD_GROUPS = {"O+","O-","A+","A-","B+","B-","AB+","AB-"};
    private static final String[] SUPPLIERS = {
        "Apollo Medicals","Cipla Distributors","Sun Pharma Depot",
        "MedLine India","HealthMart Traders","Hindustan Medicals"
    };
    private static final String[] PROCEDURES = {
        "Appendicectomy","Open Hernia Repair","Laparoscopic Cholecystectomy",
        "ORIF Femur Fracture","Knee Arthroscopy","Total Knee Replacement",
        "Thyroidectomy","Tonsillectomy","Caesarean Section",
        "Laparoscopic Hysterectomy","Cataract Extraction + IOL","Septoplasty"
    };
    private static final String[] RAD_FINDINGS = {
        "No acute cardiopulmonary process identified.",
        "Mild hepatomegaly noted. No focal lesion.",
        "Degenerative changes in lumbar vertebrae L4-L5.",
        "Right pleural effusion present, moderate in size.",
        "Diffuse brain atrophy consistent with age.",
        "Fracture line noted — callus formation present.",
        "No significant abnormality detected.",
        "Heterogeneous echogenicity of thyroid — follow-up advised."
    };
    private static final BloodGroup[] BLOOD_GROUP_ENUMS = BloodGroup.values();

    // ── Repositories ─────────────────────────────────────────────────────────

    private final PatientRepository          patientRepo;
    private final OpdVisitRepository         opdVisitRepo;
    private final AppointmentRepository      appointmentRepo;
    private final HrDepartmentRepository     hrDeptRepo;
    private final DesignationRepository      designationRepo;
    private final EmployeeRepository         employeeRepo;
    private final IpdWardRepository          wardRepo;
    private final IpdBedRepository           bedRepo;
    private final IpdAdmissionRepository     ipdAdmissionRepo;
    private final LabOrderRepository         labOrderRepo;
    private final LabTestRepository          labTestRepo;
    private final RadiologyOrderRepository   radOrderRepo;
    private final ImagingModalityRepository  modalityRepo;
    private final ImagingStudyRepository     studyRepo;
    private final MedicineCategoryRepository medCatRepo;
    private final MedicineRepository         medicineRepo;
    private final MedicineBatchRepository    batchRepo;
    private final PharmacyBillRepository     pharmacyBillRepo;
    private final IncomeEntryRepository      incomeRepo;
    private final ExpenseEntryRepository     expenseRepo;
    private final ExpenseCategoryRepository  expCatRepo;
    private final StockReceiptRepository     stockReceiptRepo;
    private final InventoryItemRepository    inventoryItemRepo;
    private final BloodDonorRepository       bloodDonorRepo;
    private final BloodUnitRepository        bloodUnitRepo;
    private final BloodRequestRepository     bloodRequestRepo;
    private final OperationTheatreRepository theatreRepo;
    private final OtScheduleRepository       otScheduleRepo;
    private final SpecializationRepository   specializationRepo;
    private final DoctorProfileRepository    doctorProfileRepo;
    private final JdbcTemplate               jdbc;

    public DemoDataSeeder(
            PatientRepository          patientRepo,
            OpdVisitRepository         opdVisitRepo,
            AppointmentRepository      appointmentRepo,
            HrDepartmentRepository     hrDeptRepo,
            DesignationRepository      designationRepo,
            EmployeeRepository         employeeRepo,
            IpdWardRepository          wardRepo,
            IpdBedRepository           bedRepo,
            IpdAdmissionRepository     ipdAdmissionRepo,
            LabOrderRepository         labOrderRepo,
            LabTestRepository          labTestRepo,
            RadiologyOrderRepository   radOrderRepo,
            ImagingModalityRepository  modalityRepo,
            ImagingStudyRepository     studyRepo,
            MedicineCategoryRepository medCatRepo,
            MedicineRepository         medicineRepo,
            MedicineBatchRepository    batchRepo,
            PharmacyBillRepository     pharmacyBillRepo,
            IncomeEntryRepository      incomeRepo,
            ExpenseEntryRepository     expenseRepo,
            ExpenseCategoryRepository  expCatRepo,
            StockReceiptRepository     stockReceiptRepo,
            InventoryItemRepository    inventoryItemRepo,
            BloodDonorRepository       bloodDonorRepo,
            BloodUnitRepository        bloodUnitRepo,
            BloodRequestRepository     bloodRequestRepo,
            OperationTheatreRepository theatreRepo,
            OtScheduleRepository       otScheduleRepo,
            SpecializationRepository   specializationRepo,
            DoctorProfileRepository    doctorProfileRepo,
            JdbcTemplate               jdbc) {
        this.patientRepo       = patientRepo;
        this.opdVisitRepo      = opdVisitRepo;
        this.appointmentRepo   = appointmentRepo;
        this.hrDeptRepo        = hrDeptRepo;
        this.designationRepo   = designationRepo;
        this.employeeRepo      = employeeRepo;
        this.wardRepo          = wardRepo;
        this.bedRepo           = bedRepo;
        this.ipdAdmissionRepo  = ipdAdmissionRepo;
        this.labOrderRepo      = labOrderRepo;
        this.labTestRepo       = labTestRepo;
        this.radOrderRepo      = radOrderRepo;
        this.modalityRepo      = modalityRepo;
        this.studyRepo         = studyRepo;
        this.medCatRepo        = medCatRepo;
        this.medicineRepo      = medicineRepo;
        this.batchRepo         = batchRepo;
        this.pharmacyBillRepo  = pharmacyBillRepo;
        this.incomeRepo        = incomeRepo;
        this.expenseRepo       = expenseRepo;
        this.expCatRepo        = expCatRepo;
        this.stockReceiptRepo  = stockReceiptRepo;
        this.inventoryItemRepo = inventoryItemRepo;
        this.bloodDonorRepo    = bloodDonorRepo;
        this.bloodUnitRepo     = bloodUnitRepo;
        this.bloodRequestRepo  = bloodRequestRepo;
        this.theatreRepo       = theatreRepo;
        this.otScheduleRepo    = otScheduleRepo;
        this.specializationRepo = specializationRepo;
        this.doctorProfileRepo  = doctorProfileRepo;
        this.jdbc              = jdbc;
    }

    // ── Entry point ──────────────────────────────────────────────────────────

    @Override
    public void run(ApplicationArguments args) {
        // NOTE: No @Transactional here on purpose.
        //
        // With @Transactional the JdbcTemplate deletes and the JPA inserts all share
        // the same connection (DataSourceUtils enrolls the JDBC connection into the
        // active JPA transaction and sets autoCommit=false). Any unchecked exception
        // from any seed method then rolls back EVERYTHING — including the deletes —
        // leaving the old data intact. Removing the outer transaction means:
        //   • JdbcTemplate statements each auto-commit immediately
        //   • Spring Data save() / saveAll() each commit in their own micro-transaction
        //   • A failure in one phase leaves partial data, but re-running the seeder
        //     cleans it up on the next pass.
        TenantContext.set(TENANT);
        try {
            log.info("╔══════════════════════════════════════════════════════════╗");
            log.info("║  DemoDataSeeder: wiping old data + generating fresh set  ║");
            log.info("╚══════════════════════════════════════════════════════════╝");

            clearDemoData();

            // Phase 1 — HR + infrastructure
            List<HrDepartment>     depts    = seedDepartments();
            List<Designation>      desigs   = seedDesignations(depts);
            List<Employee>         emps     = seedEmployees(depts, desigs);
            List<OperationTheatre> theatres = seedTheatres();
            List<IpdWard>          wards    = seedWards();
            List<IpdBed>           beds     = seedBeds(wards);

            // Phase 2 — Patients
            List<Patient> patients = seedPatients();

            // Phase 3 — Front Office + OPD
            seedAppointments(patients, emps);
            List<OpdVisit> visits = seedOpdVisits(patients, emps, depts);

            // Phase 4 — Pharmacy
            seedPharmacyData(patients);

            // Phase 5 — IPD
            List<IpdAdmission> admissions = seedIpdAdmissions(patients, emps, wards, beds, visits);

            // Phase 6 — Diagnostics
            seedLabOrders(patients, emps, visits);
            seedRadiologyData(patients, emps);

            // Phase 7 — Finance + Inventory
            seedFinanceEntries();
            seedStockReceipts();

            // Phase 8 — Blood Bank
            seedBloodBankData(patients, emps);

            // Phase 9 — OT
            seedOtSchedules(patients, admissions, emps, theatres);

            // Phase 10 — Doctor Profiles
            seedDoctorProfiles(emps, depts);

            log.info("╔══════════════════════════════════════════════════════════╗");
            log.info("║  DemoDataSeeder complete — 6-month data anchored to {}  ║", LocalDate.now());
            log.info("╚══════════════════════════════════════════════════════════╝");
        } finally {
            TenantContext.clear();
        }
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────

    private void clearDemoData() {
        log.info("[DemoSeeder] Clearing old demo data...");
        // Delete leaf tables first to satisfy FK constraints.
        // Tables with ON DELETE CASCADE (ot_consumables, lab_order_items, radiology_order_items,
        // opd_charges, prescriptions/prescription_items, ipd_charges) are handled automatically
        // when their parent is deleted.
        //
        // IMPORTANT: all deletes run on a single connection with search_path set to the tenant
        // schema.  Without this, JdbcTemplate uses the default 'public' search_path while JPA
        // (via TenantAwareDataSource) operates on the tenant schema — leaving stale tenant data
        // behind and causing duplicate-key errors on the next saveAll().
        String[] deletes = {
            "DELETE FROM ot_schedules",            // cascades → ot_consumables
            "DELETE FROM blood_issues",
            "DELETE FROM blood_requests",
            "DELETE FROM blood_units",
            "DELETE FROM blood_donors",
            "DELETE FROM lab_orders",              // cascades → lab_order_items
            "DELETE FROM radiology_orders",        // cascades → radiology_order_items
            "DELETE FROM pharmacy_bill_items",
            "DELETE FROM pharmacy_bills",
            "DELETE FROM medicine_batches",
            "DELETE FROM medicines",
            "DELETE FROM medicine_categories",
            "DELETE FROM imaging_studies",
            "DELETE FROM imaging_modalities",
            "DELETE FROM ipd_admissions",          // cascades → ipd_charges
            "DELETE FROM beds",
            "DELETE FROM wards",
            "DELETE FROM opd_tokens",
            "DELETE FROM appointments",
            "DELETE FROM opd_visits",              // cascades → opd_charges, prescriptions, prescription_items
            "DELETE FROM patients",                // hard-delete, bypasses @SQLDelete soft-delete
            "DELETE FROM attendance_records",      // FK → employees; must precede employees delete
            "DELETE FROM doctor_schedules",        // FK → doctor_profiles
            "DELETE FROM doctor_specializations",  // join table for doctor_profiles ↔ specializations
            "DELETE FROM doctor_profiles",         // FK → employees
            "DELETE FROM specializations",
            "DELETE FROM employees",               // hard-delete
            "DELETE FROM designations",
            "DELETE FROM hr_departments",
            "DELETE FROM ot_theatres",
            "DELETE FROM income_entries",
            "DELETE FROM expense_entries",
            "DELETE FROM stock_issues",
            "DELETE FROM stock_receipts",
        };
        jdbc.execute((ConnectionCallback<Void>) conn -> {
            try {
                conn.createStatement().execute("SET search_path TO " + TENANT + ", public");
            } catch (Exception ex) {
                log.warn("[DemoSeeder] Could not set search_path to '{}' — {}", TENANT, ex.getMessage());
            }
            for (String sql : deletes) {
                try {
                    conn.createStatement().execute(sql);
                } catch (Exception ex) {
                    log.warn("[DemoSeeder] Skipping failed cleanup statement: {} — {}", sql, ex.getMessage());
                }
            }
            return null;
        });
        log.info("[DemoSeeder] Old data cleared.");
    }

    // ── Phase 1: HR + Infrastructure ─────────────────────────────────────────

    private List<HrDepartment> seedDepartments() {
        record D(String name, String code) {}
        var rows = List.of(
            new D("General Medicine", "GM"),
            new D("Surgery",          "SRG"),
            new D("Orthopaedics",     "ORT"),
            new D("Gynaecology",      "GYN"),
            new D("ENT",              "ENT")
        );
        List<HrDepartment> saved = new ArrayList<>();
        for (var r : rows) {
            saved.add(hrDeptRepo.save(HrDepartment.builder().name(r.name()).code(r.code()).active(true).build()));
        }
        log.info("[DemoSeeder] {} departments", saved.size());
        return saved;
    }

    private List<Designation> seedDesignations(List<HrDepartment> depts) {
        List<Designation> saved = new ArrayList<>();
        for (HrDepartment d : depts) {
            saved.add(designationRepo.save(Designation.builder().title("Consultant").departmentId(d.getId()).active(true).build()));
            saved.add(designationRepo.save(Designation.builder().title("Senior Resident").departmentId(d.getId()).active(true).build()));
        }
        // Cross-department roles (null departmentId is allowed by the schema)
        for (String title : List.of("Head Nurse","Staff Nurse","Pharmacist","Receptionist","Accounts Officer")) {
            saved.add(designationRepo.save(Designation.builder().title(title).active(true).build()));
        }
        log.info("[DemoSeeder] {} designations", saved.size());
        return saved;
    }

    private List<Employee> seedEmployees(List<HrDepartment> depts, List<Designation> desigs) {
        // Find designation IDs by title
        Map<String, UUID> anyDesigByTitle = new LinkedHashMap<>();
        for (Designation d : desigs) { anyDesigByTitle.putIfAbsent(d.getTitle(), d.getId()); }

        // Per-department designation maps (consultant + resident per dept)
        Map<UUID, UUID> consultantByDept = new LinkedHashMap<>();
        Map<UUID, UUID> residentByDept   = new LinkedHashMap<>();
        for (Designation d : desigs) {
            if ("Consultant".equals(d.getTitle())      && d.getDepartmentId() != null) consultantByDept.put(d.getDepartmentId(), d.getId());
            if ("Senior Resident".equals(d.getTitle()) && d.getDepartmentId() != null) residentByDept.put(d.getDepartmentId(), d.getId());
        }

        record EmpData(String first, String last, boolean male, String deptName, String desigTitle) {}
        List<EmpData> empRows = List.of(
            // General Medicine
            new EmpData("Pradeep","Joshi",    true,  "General Medicine", "Consultant"),
            new EmpData("Ananya","Sharma",    false, "General Medicine", "Senior Resident"),
            new EmpData("Kavya","Pillai",     false, "General Medicine", "Head Nurse"),
            new EmpData("Sonal","Nair",       false, "General Medicine", "Staff Nurse"),
            // Surgery
            new EmpData("Suresh","Verma",     true,  "Surgery",          "Consultant"),
            new EmpData("Nitin","Patel",      true,  "Surgery",          "Senior Resident"),
            new EmpData("Divya","Reddy",      false, "Surgery",          "Head Nurse"),
            new EmpData("Rashmi","Gupta",     false, "Surgery",          "Staff Nurse"),
            // Orthopaedics
            new EmpData("Ravi","Mehta",       true,  "Orthopaedics",     "Consultant"),
            new EmpData("Vaibhav","Singh",    true,  "Orthopaedics",     "Senior Resident"),
            new EmpData("Aarti","Iyer",       false, "Orthopaedics",     "Head Nurse"),
            new EmpData("Pooja","Mishra",     false, "Orthopaedics",     "Staff Nurse"),
            // Gynaecology
            new EmpData("Meera","Trivedi",    false, "Gynaecology",      "Consultant"),
            new EmpData("Shweta","Bose",      false, "Gynaecology",      "Senior Resident"),
            new EmpData("Neha","Shah",        false, "Gynaecology",      "Head Nurse"),
            new EmpData("Ritu","Desai",       false, "Gynaecology",      "Staff Nurse"),
            // ENT
            new EmpData("Kiran","Chavan",     true,  "ENT",              "Consultant"),
            new EmpData("Rohit","Malhotra",   true,  "ENT",              "Senior Resident"),
            new EmpData("Archana","Kumar",    false, "ENT",              "Head Nurse"),
            new EmpData("Vandana","Rao",      false, "ENT",              "Staff Nurse"),
            // Admin / Support
            new EmpData("Amit","Gupta",       true,  null,               "Pharmacist"),
            new EmpData("Deepak","Joshi",     true,  null,               "Pharmacist"),
            new EmpData("Sachin","Patel",     true,  null,               "Receptionist"),
            new EmpData("Asha","Nair",        false, null,               "Receptionist"),
            new EmpData("Ajay","Sharma",      true,  null,               "Accounts Officer")
        );

        Map<String, HrDepartment> deptByName = depts.stream().collect(Collectors.toMap(HrDepartment::getName, d -> d));
        List<Employee> saved = new ArrayList<>();
        int seq = 1;
        int yr  = LocalDate.now().getYear();

        for (EmpData row : empRows) {
            HrDepartment dept = row.deptName() != null ? deptByName.get(row.deptName()) : null;
            UUID desigId;
            if (dept != null) {
                desigId = "Consultant".equals(row.desigTitle())
                    ? consultantByDept.get(dept.getId())
                    : residentByDept.getOrDefault(dept.getId(), anyDesigByTitle.get(row.desigTitle()));
                if (desigId == null) desigId = anyDesigByTitle.get(row.desigTitle());
            } else {
                desigId = anyDesigByTitle.get(row.desigTitle());
            }
            Employee e = Employee.builder()
                .employeeCode(String.format("EMP-%d-%03d", yr, seq++))
                .firstName(row.first())
                .lastName(row.last())
                .gender(row.male() ? Employee.Gender.MALE : Employee.Gender.FEMALE)
                .departmentId(dept != null ? dept.getId() : null)
                .designationId(desigId)
                .employmentType(Employee.EmploymentType.FULL_TIME)
                .joinDate(LocalDate.now().minusMonths(6 + RNG.nextInt(48)))
                .mobile("98000" + String.format("%05d", seq))
                .build();
            saved.add(employeeRepo.save(e));
        }
        log.info("[DemoSeeder] {} employees", saved.size());
        return saved;
    }

    private List<OperationTheatre> seedTheatres() {
        record T(String num, String name, OperationTheatre.TheatreType type) {}
        var rows = List.of(
            new T("OT-01", "Operation Theatre 1",  OperationTheatre.TheatreType.GENERAL),
            new T("OT-02", "Orthopaedic OT",       OperationTheatre.TheatreType.ORTHO),
            new T("OT-03", "Emergency OT",         OperationTheatre.TheatreType.EMERGENCY)
        );
        List<OperationTheatre> saved = new ArrayList<>();
        for (var r : rows) {
            saved.add(theatreRepo.save(OperationTheatre.builder()
                .theatreNumber(r.num()).name(r.name()).type(r.type()).build()));
        }
        log.info("[DemoSeeder] {} OT theatres", saved.size());
        return saved;
    }

    private List<IpdWard> seedWards() {
        record W(String name, IpdWard.WardType type, int beds) {}
        var rows = List.of(
            new W("General Ward",     IpdWard.WardType.GENERAL,    12),
            new W("Surgical Ward",    IpdWard.WardType.SURGERY,    10),
            new W("Orthopaedic Ward", IpdWard.WardType.ORTHOPEDIC,  8),
            new W("Maternity Ward",   IpdWard.WardType.MATERNITY,   6),
            new W("ICU",              IpdWard.WardType.ICU,          4)
        );
        List<IpdWard> saved = new ArrayList<>();
        for (var r : rows) {
            saved.add(wardRepo.save(IpdWard.builder()
                .name(r.name()).wardType(r.type()).totalBeds(r.beds()).active(true).build()));
        }
        log.info("[DemoSeeder] {} wards", saved.size());
        return saved;
    }

    private List<IpdBed> seedBeds(List<IpdWard> wards) {
        List<IpdBed> saved = new ArrayList<>();
        for (IpdWard w : wards) {
            for (int i = 1; i <= w.getTotalBeds(); i++) {
                IpdBed.BedType bedType = w.getWardType() == IpdWard.WardType.ICU
                    ? IpdBed.BedType.ICU
                    : (i <= 2 ? IpdBed.BedType.PRIVATE : IpdBed.BedType.GENERAL);
                BigDecimal charge = w.getWardType() == IpdWard.WardType.ICU
                    ? new BigDecimal("3000")
                    : (bedType == IpdBed.BedType.PRIVATE ? new BigDecimal("1500") : new BigDecimal("600"));
                saved.add(bedRepo.save(IpdBed.builder()
                    .wardId(w.getId())
                    .bedNumber(w.getWardType().name().substring(0, 2) + String.format("%02d", i))
                    .bedType(bedType)
                    .dailyCharge(charge)
                    .build()));
            }
        }
        log.info("[DemoSeeder] {} beds across {} wards", saved.size(), wards.size());
        return saved;
    }

    // ── Phase 2: Patients ─────────────────────────────────────────────────────

    private List<Patient> seedPatients() {
        List<Patient> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            boolean male = i < 48;
            boolean other = i >= 96;
            String first = other ? pick(M_FIRST) : (male ? pick(M_FIRST) : pick(F_FIRST));
            String last  = pick(LAST);
            int ageYears = 5 + RNG.nextInt(75);
            Patient p = Patient.builder()
                .firstName(first)
                .lastName(last)
                .dateOfBirth(LocalDate.now().minusYears(ageYears).minusDays(RNG.nextInt(365)))
                .gender(other ? Patient.Gender.OTHER : (male ? Patient.Gender.MALE : Patient.Gender.FEMALE))
                .mobile(String.format("98001%05d", i + 1))
                .email(first.toLowerCase() + "." + last.toLowerCase() + i + "@email.com")
                .address(RNG.nextInt(100) + ", " + pick(CITIES))
                .bloodGroup(BLOOD_GROUPS[i % BLOOD_GROUPS.length])
                .build();
            list.add(patientRepo.save(p));
        }
        log.info("[DemoSeeder] {} patients", list.size());
        return list;
    }

    // ── Phase 3: Front Office + OPD ──────────────────────────────────────────

    private void seedAppointments(List<Patient> patients, List<Employee> emps) {
        List<Employee> doctors = doctors(emps);
        int yr = LocalDate.now().getYear();
        List<Appointment> list = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            Patient p  = pick(patients);
            Employee dr = pick(doctors);
            boolean future = i > 40;
            LocalDate date = future ? LocalDate.now().plusDays(1 + RNG.nextInt(30))
                                    : LocalDate.now().minusDays(1 + RNG.nextInt(90));
            Appointment.AppointmentStatus status;
            if (future) {
                status = RNG.nextBoolean() ? Appointment.AppointmentStatus.SCHEDULED : Appointment.AppointmentStatus.CONFIRMED;
            } else {
                int r = RNG.nextInt(10);
                status = r < 6 ? Appointment.AppointmentStatus.COMPLETED
                       : r < 8 ? Appointment.AppointmentStatus.NO_SHOW
                       : Appointment.AppointmentStatus.CANCELLED;
            }
            Appointment a = Appointment.builder()
                .appointmentNumber(String.format("APT-%d-%05d", yr, i))
                .patientId(p.getId())
                .patientName(fullName(p))
                .patientMobile(p.getMobile())
                .doctorId(dr.getId())
                .doctorName("Dr. " + fullName(dr))
                .department(deptName(dr, emps, null))
                .appointmentDate(date)
                .timeSlot(timeSlot())
                .appointmentType(Appointment.AppointmentType.CONSULTATION)
                .build();
            a.setStatus(status);
            list.add(a);
        }
        appointmentRepo.saveAll(list);
        log.info("[DemoSeeder] {} appointments", list.size());
    }

    private List<OpdVisit> seedOpdVisits(List<Patient> patients, List<Employee> emps, List<HrDepartment> depts) {
        List<Employee> doctors = doctors(emps);
        int yr = LocalDate.now().getYear();
        List<OpdVisit> list = new ArrayList<>();
        for (int i = 1; i <= 120; i++) {
            Patient p   = pick(patients);
            Employee dr = pick(doctors);
            int r = RNG.nextInt(10);
            OpdVisit.VisitStatus   vs = r < 8 ? OpdVisit.VisitStatus.COMPLETED
                                       : r < 9 ? OpdVisit.VisitStatus.IN_PROGRESS
                                       : OpdVisit.VisitStatus.CANCELLED;
            OpdVisit.PaymentStatus ps = RNG.nextInt(10) < 7 ? OpdVisit.PaymentStatus.PAID
                                      : RNG.nextInt(2) == 0 ? OpdVisit.PaymentStatus.PENDING
                                      : OpdVisit.PaymentStatus.PARTIAL;
            BigDecimal fee = BigDecimal.valueOf(300 + RNG.nextInt(500));
            OpdVisit v = OpdVisit.builder()
                .visitNumber(String.format("OPD-%d-%05d", yr, i))
                .patientId(p.getId())
                .patientName(fullName(p))
                .visitDate(rndDate(180))
                .department(deptName(dr, emps, depts))
                .doctorId(dr.getId())
                .doctorName("Dr. " + fullName(dr))
                .symptoms(pick(SYMPTOMS))
                .consultationFee(fee)
                .build();
            v.setVisitStatus(vs);
            v.setDiagnosis(pick(DIAGNOSES));
            v.setPaymentStatus(ps);
            list.add(v);
        }
        opdVisitRepo.saveAll(list);
        log.info("[DemoSeeder] {} OPD visits", list.size());
        return list;
    }

    // ── Phase 4: Pharmacy ─────────────────────────────────────────────────────

    private void seedPharmacyData(List<Patient> patients) {
        // Medicines + categories
        record MC(String name) {}
        var catNames = List.of("Antibiotics","Analgesics & NSAIDs","Cardiovascular","Vitamins & Supplements","Antacids & GI");
        List<MedicineCategory> cats = new ArrayList<>();
        for (String name : catNames) {
            cats.add(medCatRepo.save(new MedicineCategory(name)));
        }

        record Med(String name, String generic, String unit, int catIdx) {}
        var meds = List.of(
            new Med("Amoxicillin 500mg", "Amoxicillin", "TAB", 0),
            new Med("Azithromycin 500mg", "Azithromycin", "TAB", 0),
            new Med("Ciprofloxacin 500mg", "Ciprofloxacin", "TAB", 0),
            new Med("Paracetamol 500mg", "Paracetamol", "TAB", 1),
            new Med("Ibuprofen 400mg", "Ibuprofen", "TAB", 1),
            new Med("Diclofenac 50mg", "Diclofenac", "TAB", 1),
            new Med("Amlodipine 5mg", "Amlodipine", "TAB", 2),
            new Med("Atenolol 50mg", "Atenolol", "TAB", 2),
            new Med("Metformin 500mg", "Metformin", "TAB", 2),
            new Med("Atorvastatin 10mg", "Atorvastatin", "TAB", 2),
            new Med("Vitamin B Complex", "B-Complex", "TAB", 3),
            new Med("Vitamin C 500mg", "Ascorbic Acid", "TAB", 3),
            new Med("Calcium + Vit D3", "Calcium Carbonate", "TAB", 3),
            new Med("Pantoprazole 40mg", "Pantoprazole", "TAB", 4),
            new Med("Ondansetron 4mg", "Ondansetron", "TAB", 4)
        );
        List<Medicine> medicines = new ArrayList<>();
        List<MedicineBatch> batches = new ArrayList<>();
        for (int idx = 0; idx < meds.size(); idx++) {
            var m = meds.get(idx);
            Medicine med = Medicine.builder()
                .category(cats.get(m.catIdx()))
                .name(m.name())
                .genericName(m.generic())
                .unit(m.unit())
                .reorderLevel(20)
                .build();
            med = medicineRepo.save(med);
            medicines.add(med);
            MedicineBatch b = MedicineBatch.builder()
                .medicine(med)
                .batchNumber(String.format("BCH-%d-%03d", LocalDate.now().getYear(), idx + 1))
                .expiryDate(LocalDate.now().plusMonths(12 + RNG.nextInt(24)))
                .quantity(200 + RNG.nextInt(300))
                .purchasePrice(BigDecimal.valueOf(2 + RNG.nextInt(18)))
                .salePrice(BigDecimal.valueOf(5 + RNG.nextInt(30)))
                .build();
            batches.add(batchRepo.save(b));
        }

        // Pharmacy bills
        int yr = LocalDate.now().getYear();
        for (int i = 1; i <= 35; i++) {
            Patient p = pick(patients);
            PharmacyBill bill = PharmacyBill.builder()
                .billNumber(String.format("PH-%d%02d-%03d", yr, LocalDate.now().getMonthValue(), i))
                .patientId(p.getId())
                .patientName(fullName(p))
                .paymentMode(pick(new String[]{"CASH","UPI","CARD"}))
                .build();
            int items = 1 + RNG.nextInt(3);
            Set<Integer> usedIdx = new HashSet<>();
            for (int j = 0; j < items; j++) {
                int bidx;
                do { bidx = RNG.nextInt(batches.size()); } while (usedIdx.contains(bidx));
                usedIdx.add(bidx);
                MedicineBatch batch = batches.get(bidx);
                int qty = 1 + RNG.nextInt(14) + 1;
                bill.addItem(new PharmacyBillItem(batch, batch.getMedicine().getName(), qty, batch.getSalePrice()));
            }
            bill.recalculateTotals();
            pharmacyBillRepo.save(bill);
        }
        log.info("[DemoSeeder] {} medicine categories, {} medicines, {} pharmacy bills", cats.size(), medicines.size(), 35);
    }

    // ── Phase 5: IPD ─────────────────────────────────────────────────────────

    private List<IpdAdmission> seedIpdAdmissions(List<Patient> patients, List<Employee> emps,
                                                  List<IpdWard> wards, List<IpdBed> beds,
                                                  List<OpdVisit> visits) {
        List<Employee> doctors = doctors(emps);
        int yr = LocalDate.now().getYear();
        List<IpdAdmission> saved = new ArrayList<>();

        // Use first 8 beds for active admissions, rest for discharged
        List<IpdBed> activeBeds     = new ArrayList<>(beds.subList(0, Math.min(8, beds.size())));
        List<IpdBed> availableBeds  = new ArrayList<>(beds.subList(Math.min(8, beds.size()), beds.size()));

        for (int i = 1; i <= 30; i++) {
            boolean active    = i <= 8;
            boolean discharged = i > 8 && i <= 28;
            // i > 28 → TRANSFERRED (2 records)

            Patient p   = pick(patients);
            Employee dr = pick(doctors);
            IpdBed bed  = active ? activeBeds.get(i - 1)
                                 : pick(availableBeds);
            IpdWard ward = wards.stream()
                .filter(w -> w.getId().equals(bed.getWardId()))
                .findFirst()
                .orElse(wards.get(0));

            LocalDateTime admDate = active
                ? LocalDateTime.now().minusDays(1 + RNG.nextInt(10))
                : LocalDateTime.now().minusDays(10 + RNG.nextInt(170));

            IpdAdmission adm = IpdAdmission.builder()
                .admissionNumber(String.format("IPD-%d-%05d", yr, i))
                .patientId(p.getId())
                .patientName(fullName(p))
                .wardId(ward.getId())
                .bedId(bed.getId())
                .doctorId(dr.getId())
                .doctorName("Dr. " + fullName(dr))
                .admissionDate(admDate)
                .admissionDiagnosis(pick(DIAGNOSES))
                .opdVisitId(visits.isEmpty() ? null : pick(visits).getId())
                .build();

            if (discharged) {
                adm.setStatus(IpdAdmission.AdmissionStatus.DISCHARGED);
                adm.setDischargeDate(admDate.plusDays(3 + RNG.nextInt(10)));
                adm.setFinalDiagnosis(pick(DIAGNOSES));
                adm.setConditionAtDischarge(IpdAdmission.DischargeCondition.IMPROVED);
                adm.setDischargeNotes("Patient discharged in stable condition. Advised follow-up.");
                adm.setPaymentStatus(IpdAdmission.PaymentStatus.PAID);
            } else if (i > 28) {
                adm.setStatus(IpdAdmission.AdmissionStatus.TRANSFERRED);
            } else {
                // active — status remains ADMITTED (default)
                bed.setStatus(IpdBed.BedStatus.OCCUPIED);
                bedRepo.save(bed);
            }
            saved.add(ipdAdmissionRepo.save(adm));
        }
        log.info("[DemoSeeder] {} IPD admissions (8 active, 20 discharged, 2 transferred)", saved.size());
        return saved;
    }

    // ── Phase 6: Diagnostics ─────────────────────────────────────────────────

    private void seedLabOrders(List<Patient> patients, List<Employee> emps, List<OpdVisit> visits) {
        List<LabTest>     tests   = labTestRepo.findAll();
        List<Employee>    doctors = doctors(emps);
        int yr = LocalDate.now().getYear();
        List<LabOrder> orders = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            Patient  p  = pick(patients);
            Employee dr = pick(doctors);
            int r = RNG.nextInt(10);
            LabOrder.OrderStatus   os = r < 5 ? LabOrder.OrderStatus.COMPLETED
                                       : r < 8 ? LabOrder.OrderStatus.SAMPLE_COLLECTED
                                       : LabOrder.OrderStatus.PENDING;
            LabOrder.PaymentStatus ps = os == LabOrder.OrderStatus.COMPLETED
                ? LabOrder.PaymentStatus.PAID : LabOrder.PaymentStatus.PENDING;
            LabOrder order = LabOrder.builder()
                .orderNumber(String.format("LAB-%d-%05d", yr, i))
                .patientId(p.getId())
                .patientName(fullName(p))
                .patientMobile(p.getMobile())
                .referredById(dr.getId())
                .referredByName("Dr. " + fullName(dr))
                .sourceType(LabOrder.SourceType.OPD)
                .priority(LabOrder.Priority.ROUTINE)
                .build();
            order.setStatus(os);
            order.setPaymentStatus(ps);
            if (os != LabOrder.OrderStatus.PENDING) {
                order.setSampleCollectedAt(LocalDateTime.now().minusDays(RNG.nextInt(90)));
            }
            // 1-2 tests per order
            int numTests = 1 + RNG.nextInt(2);
            Set<Integer> used = new HashSet<>();
            for (int t = 0; t < numTests && !tests.isEmpty(); t++) {
                int ti;
                do { ti = RNG.nextInt(tests.size()); } while (used.contains(ti));
                used.add(ti);
                LabTest test = tests.get(ti);
                LabOrderItem item = new LabOrderItem(order, test);
                if (os == LabOrder.OrderStatus.COMPLETED) {
                    item.setStatus(LabOrderItem.ItemStatus.COMPLETED);
                    item.setResult("Within normal limits");
                    item.setResultEnteredAt(LocalDateTime.now().minusDays(RNG.nextInt(30)));
                    item.setResultEnteredBy("Dr. " + fullName(pick(doctors)));
                }
                order.getItems().add(item);
            }
            order.recalculateTotals();
            orders.add(order);
        }
        labOrderRepo.saveAll(orders);
        log.info("[DemoSeeder] {} lab orders", orders.size());
    }

    private void seedRadiologyData(List<Patient> patients, List<Employee> emps) {
        // Modalities
        record Mod(String name, String code) {}
        var modRows = List.of(
            new Mod("X-Ray",      "XR"),
            new Mod("Ultrasound", "USG"),
            new Mod("CT Scan",    "CT"),
            new Mod("MRI",        "MRI")
        );
        List<ImagingModality> modalities = new ArrayList<>();
        for (var m : modRows) {
            modalities.add(modalityRepo.save(ImagingModality.builder()
                .name(m.name()).code(m.code()).description(m.name() + " imaging").build()));
        }

        // Studies (3 per modality)
        record St(String code, String name, int modIdx, BigDecimal price) {}
        var studyRows = List.of(
            new St("XR-CHEST",    "Chest PA View",             0, new BigDecimal("400")),
            new St("XR-PELVIS",   "Pelvis AP View",            0, new BigDecimal("350")),
            new St("XR-KNEE",     "Knee AP & Lateral",         0, new BigDecimal("350")),
            new St("USG-ABD",     "USG Abdomen & Pelvis",      1, new BigDecimal("700")),
            new St("USG-THYROID", "USG Thyroid Neck",          1, new BigDecimal("600")),
            new St("USG-UPPER",   "USG Upper Abdomen",         1, new BigDecimal("600")),
            new St("CT-BRAIN",    "CT Brain Plain",            2, new BigDecimal("3500")),
            new St("CT-ABD",      "CT Abdomen + Pelvis",       2, new BigDecimal("5000")),
            new St("CT-CHEST",    "CT Chest HRCT",             2, new BigDecimal("4500")),
            new St("MRI-BRAIN",   "MRI Brain with Contrast",   3, new BigDecimal("8000")),
            new St("MRI-SPINE",   "MRI Lumbar Spine",          3, new BigDecimal("7000")),
            new St("MRI-KNEE",    "MRI Knee Joint",            3, new BigDecimal("7500"))
        );
        List<ImagingStudy> studies = new ArrayList<>();
        for (var s : studyRows) {
            ImagingModality mod = modalities.get(s.modIdx());
            studies.add(studyRepo.save(ImagingStudy.builder()
                .code(s.code()).name(s.name())
                .modalityId(mod.getId())
                .price(s.price())
                .build()));
        }

        // Radiology orders
        List<Employee> doctors = doctors(emps);
        int yr = LocalDate.now().getYear();
        List<RadiologyOrder> orders = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Patient  p   = pick(patients);
            Employee dr  = pick(doctors);
            ImagingStudy study = pick(studies);
            ImagingModality mod = modalities.stream()
                .filter(m -> m.getId().equals(study.getModalityId()))
                .findFirst().orElse(modalities.get(0));
            int r = RNG.nextInt(10);
            RadiologyOrder.OrderStatus os = r < 6 ? RadiologyOrder.OrderStatus.COMPLETED
                                           : r < 8 ? RadiologyOrder.OrderStatus.IN_PROGRESS
                                           : RadiologyOrder.OrderStatus.PENDING;
            RadiologyOrder order = RadiologyOrder.builder()
                .orderNumber(String.format("RAD-%d-%05d", yr, i))
                .patientId(p.getId())
                .patientName(fullName(p))
                .patientMobile(p.getMobile())
                .referredById(dr.getId())
                .referredByName("Dr. " + fullName(dr))
                .sourceType(RadiologyOrder.SourceType.OPD)
                .priority(RadiologyOrder.Priority.ROUTINE)
                .scheduledAt(rndDatetime(60))
                .build();
            order.setStatus(os);
            order.setPaymentStatus(os == RadiologyOrder.OrderStatus.COMPLETED
                ? RadiologyOrder.PaymentStatus.PAID : RadiologyOrder.PaymentStatus.PENDING);
            RadiologyOrderItem item = new RadiologyOrderItem(order, study, mod.getName());
            if (os == RadiologyOrder.OrderStatus.COMPLETED) {
                item.setStatus(RadiologyOrderItem.ItemStatus.REPORTED);
                item.setFindings(pick(RAD_FINDINGS));
                item.setImpression("Impression: " + pick(DIAGNOSES) + " suspected.");
                item.setReportedAt(rndDatetime(30));
                item.setReportedBy("Dr. " + fullName(pick(doctors)));
            }
            order.getItems().add(item);
            order.recalculateTotals();
            orders.add(order);
        }
        radOrderRepo.saveAll(orders);
        log.info("[DemoSeeder] {} modalities, {} studies, {} radiology orders", modalities.size(), studies.size(), orders.size());
    }

    // ── Phase 7: Finance + Inventory ─────────────────────────────────────────

    private void seedFinanceEntries() {
        List<ExpenseCategory> expCats = expCatRepo.findAll();
        int yr = LocalDate.now().getYear();
        List<IncomeEntry> incomes = new ArrayList<>();
        IncomeEntry.SourceType[] srcTypes = IncomeEntry.SourceType.values();
        for (int i = 1; i <= 25; i++) {
            incomes.add(IncomeEntry.builder()
                .entryNumber(String.format("INC-%d-%05d", yr, i))
                .entryDate(rndDate(180))
                .sourceType(pickEnum(srcTypes))
                .amount(rndBd(500, 50000))
                .description(pick(new String[]{"OPD consultation fees","IPD bed charges",
                    "Pharmacy sales","Lab test fees","Radiology charges","Procedure fees"}))
                .paymentMode(pickEnum(IncomeEntry.PaymentMode.values()))
                .receivedBy("Accounts Dept")
                .build());
        }
        incomeRepo.saveAll(incomes);

        List<ExpenseEntry> expenses = new ArrayList<>();
        for (int i = 1; i <= 20 && !expCats.isEmpty(); i++) {
            ExpenseCategory cat = expCats.get(i % expCats.size());
            expenses.add(ExpenseEntry.builder()
                .entryNumber(String.format("EXP-%d-%05d", yr, i))
                .entryDate(rndDate(180))
                .categoryId(cat.getId())
                .categoryName(cat.getName())
                .description(cat.getName() + " — monthly payment")
                .amount(rndBd(1000, 80000))
                .paymentMode(pickEnum(ExpenseEntry.PaymentMode.values()))
                .approvedBy("Hospital Admin")
                .build());
        }
        expenseRepo.saveAll(expenses);
        log.info("[DemoSeeder] {} income entries, {} expense entries", incomes.size(), expenses.size());
    }

    private void seedStockReceipts() {
        List<InventoryItem> items = inventoryItemRepo.findAll();
        if (items.isEmpty()) return;
        int yr = LocalDate.now().getYear();
        List<StockReceipt> receipts = new ArrayList<>();
        int seq = 1;
        for (InventoryItem item : items) {
            int recs = 1 + RNG.nextInt(2);
            for (int j = 0; j < recs; j++) {
                int qty = 20 + RNG.nextInt(80);
                BigDecimal unitCost = rndBd(10, 200);
                receipts.add(StockReceipt.builder()
                    .receiptNumber(String.format("SR-%d-%05d", yr, seq++))
                    .entryDate(rndDate(120))
                    .itemId(item.getId())
                    .itemName(item.getName())
                    .itemUnit(item.getUnit())
                    .quantity(qty)
                    .unitCost(unitCost)
                    .totalCost(unitCost.multiply(BigDecimal.valueOf(qty)))
                    .supplierName(pick(SUPPLIERS))
                    .grnNumber(String.format("GRN-%d-%04d", yr, seq))
                    .receivedBy("Store Keeper")
                    .build());
            }
        }
        stockReceiptRepo.saveAll(receipts);
        log.info("[DemoSeeder] {} stock receipts for {} inventory items", receipts.size(), items.size());
    }

    // ── Phase 8: Blood Bank ───────────────────────────────────────────────────

    private void seedBloodBankData(List<Patient> patients, List<Employee> emps) {
        int yr = LocalDate.now().getYear();
        // Donors
        record DS(String first, String last, BloodDonor.Gender g, BloodGroup bg) {}
        var donorSeeds = List.of(
            new DS("Arjun",   "Sharma",  BloodDonor.Gender.MALE,   BloodGroup.O_POS),
            new DS("Priya",   "Nair",    BloodDonor.Gender.FEMALE, BloodGroup.A_POS),
            new DS("Rahul",   "Verma",   BloodDonor.Gender.MALE,   BloodGroup.B_POS),
            new DS("Sunita",  "Patel",   BloodDonor.Gender.FEMALE, BloodGroup.AB_POS),
            new DS("Karthik", "Menon",   BloodDonor.Gender.MALE,   BloodGroup.O_NEG),
            new DS("Deepa",   "Reddy",   BloodDonor.Gender.FEMALE, BloodGroup.A_NEG),
            new DS("Vijay",   "Kumar",   BloodDonor.Gender.MALE,   BloodGroup.B_NEG),
            new DS("Anita",   "Joshi",   BloodDonor.Gender.FEMALE, BloodGroup.AB_NEG),
            new DS("Ravi",    "Iyer",    BloodDonor.Gender.MALE,   BloodGroup.O_POS),
            new DS("Meera",   "Gupta",   BloodDonor.Gender.FEMALE, BloodGroup.A_POS)
        );
        List<BloodDonor> donors = new ArrayList<>();
        for (int i = 0; i < donorSeeds.size(); i++) {
            var d = donorSeeds.get(i);
            donors.add(bloodDonorRepo.save(BloodDonor.builder()
                .donorNumber(String.format("DON-%d-%05d", yr, i + 1))
                .firstName(d.first()).lastName(d.last())
                .gender(d.g())
                .dateOfBirth(LocalDate.now().minusYears(20 + RNG.nextInt(30)))
                .bloodGroup(d.bg())
                .mobile(String.format("97000%05d", i + 1))
                .build()));
        }

        // Blood units (20 total: 16 AVAILABLE + 4 PENDING_TESTING)
        List<BloodUnit> units = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            BloodDonor donor = donors.get(i % donors.size());
            boolean pending = i >= 16;
            units.add(bloodUnitRepo.save(BloodUnit.builder()
                .unitNumber(String.format("BU-%d-%05d", yr, i + 1))
                .bloodGroup(donor.getBloodGroup())
                .donorId(donor.getId())
                .donorName(donor.getFirstName() + " " + donor.getLastName())
                .componentType(ComponentType.WHOLE_BLOOD)
                .volumeMl(450)
                .collectionDate(LocalDate.now().minusDays(2 + RNG.nextInt(30)))
                .expiryDate(LocalDate.now().plusDays(pending ? 42 : 10 + RNG.nextInt(32)))
                .testingStatus(pending ? BloodUnit.TestingStatus.PENDING : BloodUnit.TestingStatus.CLEARED)
                .status(pending ? BloodUnit.UnitStatus.PENDING_TESTING : BloodUnit.UnitStatus.AVAILABLE)
                .build()));
        }

        // Blood requests (10)
        List<Employee> doctors = doctors(emps);
        for (int i = 1; i <= 10; i++) {
            Patient p  = pick(patients);
            BloodGroup bg = BLOOD_GROUP_ENUMS[RNG.nextInt(BLOOD_GROUP_ENUMS.length)];
            BloodRequest.RequestStatus rs = i <= 5 ? BloodRequest.RequestStatus.FULFILLED
                                          : i <= 8 ? BloodRequest.RequestStatus.PENDING
                                          : BloodRequest.RequestStatus.CANCELLED;
            BloodRequest req = BloodRequest.builder()
                .requestNumber(String.format("BR-%d-%05d", yr, i))
                .requestDate(rndDate(90))
                .patientId(p.getId())
                .patientName(fullName(p))
                .requestedBy("Dr. " + fullName(pick(doctors)))
                .bloodGroup(bg)
                .componentType(ComponentType.WHOLE_BLOOD)
                .unitsRequired(1 + RNG.nextInt(2))
                .urgency(RNG.nextInt(3) == 0 ? BloodRequest.Urgency.URGENT : BloodRequest.Urgency.ROUTINE)
                .build();
            req.setStatus(rs);
            if (rs == BloodRequest.RequestStatus.FULFILLED) req.setUnitsIssued(req.getUnitsRequired());
            bloodRequestRepo.save(req);
        }
        log.info("[DemoSeeder] {} blood donors, {} units, {} requests", donors.size(), units.size(), 10);
    }

    // ── Phase 9: OT Schedules ─────────────────────────────────────────────────

    private void seedOtSchedules(List<Patient> patients, List<IpdAdmission> admissions,
                                  List<Employee> emps, List<OperationTheatre> theatres) {
        List<Employee> consultants = emps.stream()
            .filter(e -> {
                // Consultants: Suresh Verma, Ravi Mehta, Pradeep Joshi, Meera Trivedi, Kiran Chavan
                String n = e.getFirstName();
                return List.of("Suresh","Ravi","Pradeep","Meera","Kiran").contains(n);
            }).collect(Collectors.toList());
        List<Employee> residents = emps.stream()
            .filter(e -> {
                String n = e.getFirstName();
                return List.of("Nitin","Vaibhav","Ananya","Shweta","Rohit").contains(n);
            }).collect(Collectors.toList());
        if (consultants.isEmpty()) consultants = doctors(emps);
        if (residents.isEmpty())   residents   = consultants;

        List<IpdAdmission> discharged = admissions.stream()
            .filter(a -> a.getStatus() == IpdAdmission.AdmissionStatus.DISCHARGED)
            .collect(Collectors.toList());
        List<IpdAdmission> active = admissions.stream()
            .filter(a -> a.getStatus() == IpdAdmission.AdmissionStatus.ADMITTED)
            .collect(Collectors.toList());

        int yr = LocalDate.now().getYear();
        for (int i = 1; i <= 15; i++) {
            boolean completed = i <= 10;
            boolean scheduled = i <= 13;
            Employee surgeon     = pick(consultants);
            Employee anesthetist = pick(residents);
            OperationTheatre ot  = pick(theatres);

            IpdAdmission adm;
            Patient p;
            if (completed && !discharged.isEmpty()) {
                adm = discharged.get((i - 1) % discharged.size());
                p   = patients.stream().filter(pt -> pt.getId().equals(adm.getPatientId()))
                          .findFirst().orElse(pick(patients));
            } else if (!active.isEmpty()) {
                adm = active.get(RNG.nextInt(active.size()));
                p   = patients.stream().filter(pt -> pt.getId().equals(adm.getPatientId()))
                          .findFirst().orElse(pick(patients));
            } else {
                adm = null;
                p   = pick(patients);
            }

            LocalDate schedDate = completed
                ? rndDate(90)
                : (scheduled ? LocalDate.now().plusDays(1 + RNG.nextInt(7)) : rndDate(30));
            LocalDateTime start = schedDate.atTime(8 + RNG.nextInt(4), 0);

            OtSchedule sched = OtSchedule.builder()
                .scheduleNumber(String.format("OT-%d-%05d", yr, i))
                .admissionId(adm != null ? adm.getId() : null)
                .patientId(p.getId())
                .patientName(fullName(p))
                .theatreId(ot.getId())
                .theatreName(ot.getName())
                .scheduledDate(schedDate)
                .scheduledStart(start)
                .estimatedDurationMins(60 + RNG.nextInt(120))
                .procedureName(pick(PROCEDURES))
                .operationType(OtSchedule.OperationType.ELECTIVE)
                .priority(OtSchedule.Priority.ROUTINE)
                .surgeonId(surgeon.getId())
                .surgeonName("Dr. " + fullName(surgeon))
                .anesthetistId(anesthetist.getId())
                .anesthetistName("Dr. " + fullName(anesthetist))
                .preOpDiagnosis(pick(DIAGNOSES))
                .build();

            if (completed) {
                sched.setStatus(OtSchedule.Status.COMPLETED);
                sched.setActualStart(start.plusMinutes(5));
                sched.setActualEnd(start.plusMinutes(65 + RNG.nextInt(60)));
                sched.setAnesthesiaType(OtSchedule.AnesthesiaType.GENERAL);
                sched.setPostOpDiagnosis(pick(DIAGNOSES));
                sched.setProcedureDetails("Procedure performed without complications.");
                sched.setOutcome(OtSchedule.Outcome.SUCCESSFUL);
                sched.setPatientConditionAfter(OtSchedule.PatientCondition.STABLE);
            } else if (!scheduled) {
                sched.setStatus(OtSchedule.Status.CANCELLED);
            }
            // else: default SCHEDULED
            otScheduleRepo.save(sched);
        }
        log.info("[DemoSeeder] 15 OT schedules (10 completed, 3 scheduled, 2 cancelled)");
    }

    // ── Phase 10: Doctor Profiles ─────────────────────────────────────────────

    private void seedDoctorProfiles(List<Employee> emps, List<HrDepartment> depts) {
        // 5 specializations matching the 5 departments
        record SP(String name, String code) {}
        var specRows = List.of(
            new SP("General Medicine", "GM"),
            new SP("Surgery",          "SRG"),
            new SP("Orthopaedics",     "ORT"),
            new SP("Gynaecology",      "GYN"),
            new SP("ENT",              "ENT")
        );
        Map<String, Specialization> specByCode = new LinkedHashMap<>();
        for (var r : specRows) {
            Specialization s = Specialization.builder()
                .name(r.name()).code(r.code()).active(true).build();
            specByCode.put(r.code(), specializationRepo.save(s));
        }

        // Doctor employees: the 10 named doctors (Consultants + Senior Residents)
        List<String> drNames = List.of("Pradeep","Ananya","Suresh","Nitin","Ravi","Vaibhav","Meera","Shweta","Kiran","Rohit");

        // Map from doctor first name to their department code for specialization assignment
        Map<String, String> drToSpecCode = Map.of(
            "Pradeep", "GM",  "Ananya", "GM",
            "Suresh",  "SRG", "Nitin",  "SRG",
            "Ravi",    "ORT", "Vaibhav","ORT",
            "Meera",   "GYN", "Shweta", "GYN",
            "Kiran",   "ENT", "Rohit",  "ENT"
        );

        List<Employee> doctors = emps.stream()
            .filter(e -> drNames.contains(e.getFirstName()))
            .collect(Collectors.toList());

        int count = 0;
        for (Employee e : doctors) {
            String specCode = drToSpecCode.get(e.getFirstName());
            Specialization spec = specCode != null ? specByCode.get(specCode) : null;

            DoctorProfile profile = DoctorProfile.builder()
                .employeeId(e.getId())
                .experienceYears(2 + RNG.nextInt(20))
                .consultationFee(new BigDecimal(String.valueOf(300 + RNG.nextInt(700))))
                .followUpFee(new BigDecimal(String.valueOf(150 + RNG.nextInt(350))))
                .teleConsultationFee(new BigDecimal(String.valueOf(200 + RNG.nextInt(400))))
                .onlineBookingEnabled(true)
                .displayOnPortal(true)
                .build();

            if (spec != null) {
                profile.setSpecializations(new HashSet<>(Set.of(spec)));
            }
            doctorProfileRepo.save(profile);
            count++;
        }
        log.info("[DemoSeeder] {} specializations, {} doctor profiles", specRows.size(), count);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String pick(String[] arr)          { return arr[RNG.nextInt(arr.length)]; }
    private BloodGroup pick(BloodGroup[] arr)  { return arr[RNG.nextInt(arr.length)]; }
    private <T> T pick(List<T> list)           { return list.get(RNG.nextInt(list.size())); }
    private <E extends Enum<E>> E pickEnum(E[] arr) { return arr[RNG.nextInt(arr.length)]; }

    private LocalDate     rndDate(int maxDaysAgo)     { return LocalDate.now().minusDays(1 + RNG.nextInt(maxDaysAgo)); }
    private LocalDateTime rndDatetime(int maxDaysAgo) { return rndDate(maxDaysAgo).atTime(8 + RNG.nextInt(10), RNG.nextInt(4) * 15); }

    private BigDecimal rndBd(int min, int max) { return BigDecimal.valueOf(min + RNG.nextInt(max - min)); }

    private String fullName(Patient p)   { return p.getFirstName() + " " + p.getLastName(); }
    private String fullName(Employee e)  { return e.getFirstName() + " " + e.getLastName(); }

    private String timeSlot() {
        String[] slots = {"09:00","09:30","10:00","10:30","11:00","11:30","14:00","14:30","15:00","15:30"};
        return pick(slots);
    }

    private List<Employee> doctors(List<Employee> emps) {
        // Employees whose first names match the doctor list
        List<String> drNames = List.of("Pradeep","Ananya","Suresh","Nitin","Ravi","Vaibhav","Meera","Shweta","Kiran","Rohit");
        List<Employee> docs  = emps.stream().filter(e -> drNames.contains(e.getFirstName())).collect(Collectors.toList());
        return docs.isEmpty() ? emps : docs;
    }

    private String deptName(Employee dr, List<Employee> emps, List<HrDepartment> depts) {
        // Map doctor first names to department names
        Map<String, String> map = Map.of(
            "Pradeep", "General Medicine", "Ananya", "General Medicine",
            "Suresh",  "Surgery",          "Nitin",  "Surgery",
            "Ravi",    "Orthopaedics",     "Vaibhav","Orthopaedics",
            "Meera",   "Gynaecology",      "Shweta", "Gynaecology",
            "Kiran",   "ENT",              "Rohit",  "ENT"
        );
        return map.getOrDefault(dr.getFirstName(), "General Medicine");
    }

}
