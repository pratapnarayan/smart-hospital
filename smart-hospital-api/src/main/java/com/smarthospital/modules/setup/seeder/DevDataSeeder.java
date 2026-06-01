package com.smarthospital.modules.setup.seeder;

import com.smarthospital.core.tenant.TenantContext;
import com.smarthospital.modules.auth.domain.Role;
import com.smarthospital.modules.auth.domain.User;
import com.smarthospital.modules.auth.repository.UserRepository;
import com.smarthospital.modules.bloodbank.domain.BloodDonor;
import com.smarthospital.modules.bloodbank.domain.BloodDonor.Gender;
import com.smarthospital.modules.bloodbank.domain.BloodGroup;
import com.smarthospital.modules.bloodbank.domain.BloodUnit;
import com.smarthospital.modules.bloodbank.domain.BloodUnit.TestingStatus;
import com.smarthospital.modules.bloodbank.domain.BloodUnit.UnitStatus;
import com.smarthospital.modules.bloodbank.domain.ComponentType;
import com.smarthospital.modules.bloodbank.repository.BloodDonorRepository;
import com.smarthospital.modules.bloodbank.repository.BloodUnitRepository;
import com.smarthospital.modules.finance.domain.ExpenseCategory;
import com.smarthospital.modules.finance.repository.ExpenseCategoryRepository;
import com.smarthospital.modules.inventory.domain.InventoryItem;
import com.smarthospital.modules.inventory.domain.ItemCategory;
import com.smarthospital.modules.inventory.repository.InventoryItemRepository;
import com.smarthospital.modules.inventory.repository.ItemCategoryRepository;
import com.smarthospital.modules.pathology.domain.LabTest;
import com.smarthospital.modules.pathology.domain.LabTestCategory;
import com.smarthospital.modules.pathology.repository.LabTestCategoryRepository;
import com.smarthospital.modules.pathology.repository.LabTestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Runs once on startup in the "dev" profile.
 *
 * Seeds two users:
 *
 *   SUPER_ADMIN in public schema — for calling /api/platform/tenants
 *     Email:    superadmin@smarthospital.com
 *     Password: SuperAdmin@1234
 *
 *   ADMIN in hospital_001 schema — for day-to-day dev testing
 *     Email:    admin@hospital001.com
 *     Password: Admin@1234
 */
@Component
@Profile("dev")
@Order(2)           // must run after TenantMigrationService (Order 1)
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    // Super-admin lives in public schema (no tenant context)
    private static final String SUPER_ADMIN_EMAIL = "superadmin@smarthospital.com";
    private static final String SUPER_ADMIN_PASS  = "SuperAdmin@1234";
    private static final String SUPER_ADMIN_TENANT = "public";

    // Tenant-level admin lives in hospital_001 schema
    private static final String DEV_TENANT  = "hospital_001";
    private static final String ADMIN_EMAIL = "admin@hospital001.com";
    private static final String ADMIN_PASS  = "Admin@1234";

    private final UserRepository            userRepository;
    private final PasswordEncoder           passwordEncoder;
    private final LabTestCategoryRepository labTestCategoryRepository;
    private final LabTestRepository         labTestRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ItemCategoryRepository    itemCategoryRepository;
    private final InventoryItemRepository   inventoryItemRepository;
    private final BloodDonorRepository      bloodDonorRepository;
    private final BloodUnitRepository       bloodUnitRepository;

    public DevDataSeeder(UserRepository            userRepository,
                         PasswordEncoder           passwordEncoder,
                         LabTestCategoryRepository labTestCategoryRepository,
                         LabTestRepository         labTestRepository,
                         ExpenseCategoryRepository expenseCategoryRepository,
                         ItemCategoryRepository    itemCategoryRepository,
                         InventoryItemRepository   inventoryItemRepository,
                         BloodDonorRepository      bloodDonorRepository,
                         BloodUnitRepository       bloodUnitRepository) {
        this.userRepository            = userRepository;
        this.passwordEncoder           = passwordEncoder;
        this.labTestCategoryRepository = labTestCategoryRepository;
        this.labTestRepository         = labTestRepository;
        this.expenseCategoryRepository = expenseCategoryRepository;
        this.itemCategoryRepository    = itemCategoryRepository;
        this.inventoryItemRepository   = inventoryItemRepository;
        this.bloodDonorRepository      = bloodDonorRepository;
        this.bloodUnitRepository       = bloodUnitRepository;
    }

    // NOTE: No @Transactional here — same reason as DemoDataSeeder.
    // With @Transactional, the connection is acquired at the start of run() when
    // TenantContext is null, which sets search_path TO public for the entire call.
    // Every subsequent TenantContext.set("hospital_001") inside a seed method is
    // then ignored because the connection is already bound to the transaction.
    // Without @Transactional, each save() / count() call opens its own transaction,
    // gets a fresh connection from TenantAwareDataSource, and search_path is set
    // correctly from the current TenantContext at that moment.
    @Override
    public void run(ApplicationArguments args) {
        seedSuperAdmin();
        seedTenantAdmin();
        seedPathologyData();
        seedFinanceData();
        seedInventoryData();
        seedBloodBankData();
    }

    // ── Super-admin (public schema) ──────────────────────────────────────────────

    private void seedSuperAdmin() {
        TenantContext.set(SUPER_ADMIN_TENANT);
        try {
            if (userRepository.existsByEmailIgnoreCase(SUPER_ADMIN_EMAIL)) {
                log.info("[DevSeeder] Super-admin already exists — skipping.");
                return;
            }
            User superAdmin = User.builder()
                    .email(SUPER_ADMIN_EMAIL)
                    .passwordHash(passwordEncoder.encode(SUPER_ADMIN_PASS))
                    .firstName("Platform")
                    .lastName("Admin")
                    .tenantId(SUPER_ADMIN_TENANT)
                    .role(Role.SUPER_ADMIN)
                    .active(true)
                    .build();
            userRepository.save(superAdmin);
            log.info("===========================================================");
            log.info("[DevSeeder] Super-admin created.");
            log.info("  Email   : {}", SUPER_ADMIN_EMAIL);
            log.info("  Password: {}", SUPER_ADMIN_PASS);
            log.info("  Use for : POST /api/platform/tenants");
            log.info("===========================================================");
        } finally {
            TenantContext.clear();
        }
    }

    // ── Tenant admin (hospital_001 schema) ───────────────────────────────────────

    private void seedTenantAdmin() {
        TenantContext.set(DEV_TENANT);
        try {
            if (userRepository.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
                log.info("[DevSeeder] Tenant admin already exists — skipping.");
                return;
            }
            User admin = User.builder()
                    .email(ADMIN_EMAIL)
                    .passwordHash(passwordEncoder.encode(ADMIN_PASS))
                    .firstName("Hospital")
                    .lastName("Admin")
                    .tenantId(DEV_TENANT)
                    .role(Role.ADMIN)
                    .active(true)
                    .build();
            userRepository.save(admin);
            log.info("===========================================================");
            log.info("[DevSeeder] Tenant admin created.");
            log.info("  Email   : {}", ADMIN_EMAIL);
            log.info("  Password: {}", ADMIN_PASS);
            log.info("  Tenant  : {}", DEV_TENANT);
            log.info("  Use for : all /api/v1/** endpoints");
            log.info("===========================================================");
        } finally {
            TenantContext.clear();
        }
    }

    // ── Pathology test catalog (hospital_001 schema) ─────────────────────────────

    private void seedPathologyData() {
        TenantContext.set(DEV_TENANT);
        try {
            if (labTestCategoryRepository.count() > 0) {
                log.info("[DevSeeder] Pathology data already exists — skipping.");
                return;
            }

            LabTestCategory haematology = labTestCategoryRepository.save(
                    LabTestCategory.builder().name("Haematology")
                            .description("Blood cell and coagulation studies").build());
            LabTestCategory biochemistry = labTestCategoryRepository.save(
                    LabTestCategory.builder().name("Biochemistry")
                            .description("Chemical analysis of blood and body fluids").build());
            LabTestCategory microbiology = labTestCategoryRepository.save(
                    LabTestCategory.builder().name("Microbiology")
                            .description("Bacterial, viral, and fungal studies").build());
            LabTestCategory immunology = labTestCategoryRepository.save(
                    LabTestCategory.builder().name("Immunology & Serology")
                            .description("Antibody and antigen detection").build());

            // ── Haematology ────────────────────────────────────────────────────
            labTestRepository.save(LabTest.builder()
                    .code("CBC").name("Complete Blood Count")
                    .categoryId(haematology.getId())
                    .price(new BigDecimal("250")).turnaroundHours(4)
                    .unit("cells/μL").normalRange("WBC 4.5–11.0 × 10³/μL").build());
            labTestRepository.save(LabTest.builder()
                    .code("HB").name("Haemoglobin")
                    .categoryId(haematology.getId())
                    .price(new BigDecimal("80")).turnaroundHours(2)
                    .unit("g/dL").normalRange("M: 13.5–17.5 | F: 12.0–15.5").build());
            labTestRepository.save(LabTest.builder()
                    .code("ESR").name("Erythrocyte Sedimentation Rate")
                    .categoryId(haematology.getId())
                    .price(new BigDecimal("100")).turnaroundHours(2)
                    .unit("mm/hr").normalRange("M: 0–15 | F: 0–20").build());
            labTestRepository.save(LabTest.builder()
                    .code("PT-INR").name("Prothrombin Time / INR")
                    .categoryId(haematology.getId())
                    .price(new BigDecimal("200")).turnaroundHours(4)
                    .unit("seconds").normalRange("11–13.5 sec (INR 0.8–1.1)").build());

            // ── Biochemistry ───────────────────────────────────────────────────
            labTestRepository.save(LabTest.builder()
                    .code("FBS").name("Fasting Blood Sugar")
                    .categoryId(biochemistry.getId())
                    .price(new BigDecimal("80")).turnaroundHours(2)
                    .unit("mg/dL").normalRange("70–100").build());
            labTestRepository.save(LabTest.builder()
                    .code("RBS").name("Random Blood Sugar")
                    .categoryId(biochemistry.getId())
                    .price(new BigDecimal("70")).turnaroundHours(1)
                    .unit("mg/dL").normalRange("<200").build());
            labTestRepository.save(LabTest.builder()
                    .code("HBA1C").name("HbA1c (Glycated Haemoglobin)")
                    .categoryId(biochemistry.getId())
                    .price(new BigDecimal("400")).turnaroundHours(8)
                    .unit("%").normalRange("4.0–5.6% (non-diabetic)").build());
            labTestRepository.save(LabTest.builder()
                    .code("LFT").name("Liver Function Test")
                    .categoryId(biochemistry.getId())
                    .price(new BigDecimal("500")).turnaroundHours(6)
                    .unit("U/L").normalRange("ALT 7–56 | AST 10–40 | ALP 44–147").build());
            labTestRepository.save(LabTest.builder()
                    .code("KFT").name("Kidney Function Test")
                    .categoryId(biochemistry.getId())
                    .price(new BigDecimal("450")).turnaroundHours(6)
                    .unit("mg/dL").normalRange("Creatinine 0.7–1.3 | Urea 7–25").build());
            labTestRepository.save(LabTest.builder()
                    .code("LIPID").name("Lipid Profile")
                    .categoryId(biochemistry.getId())
                    .price(new BigDecimal("600")).turnaroundHours(6)
                    .unit("mg/dL").normalRange("Total Cholesterol <200 | LDL <100").build());
            labTestRepository.save(LabTest.builder()
                    .code("TSH").name("Thyroid Stimulating Hormone")
                    .categoryId(biochemistry.getId())
                    .price(new BigDecimal("350")).turnaroundHours(8)
                    .unit("mIU/L").normalRange("0.4–4.0").build());

            // ── Microbiology ───────────────────────────────────────────────────
            labTestRepository.save(LabTest.builder()
                    .code("URINE-RE").name("Urine Routine Examination")
                    .categoryId(microbiology.getId())
                    .price(new BigDecimal("100")).turnaroundHours(2)
                    .unit(null).normalRange("No cells, casts, or bacteria").build());
            labTestRepository.save(LabTest.builder()
                    .code("URINE-CS").name("Urine Culture & Sensitivity")
                    .categoryId(microbiology.getId())
                    .price(new BigDecimal("400")).turnaroundHours(48)
                    .unit(null).normalRange("No growth").build());
            labTestRepository.save(LabTest.builder()
                    .code("BLOOD-CS").name("Blood Culture & Sensitivity")
                    .categoryId(microbiology.getId())
                    .price(new BigDecimal("600")).turnaroundHours(72)
                    .unit(null).normalRange("No growth").build());

            // ── Immunology ─────────────────────────────────────────────────────
            labTestRepository.save(LabTest.builder()
                    .code("WIDAL").name("Widal Test")
                    .categoryId(immunology.getId())
                    .price(new BigDecimal("150")).turnaroundHours(4)
                    .unit(null).normalRange("Titre < 1:80").build());
            labTestRepository.save(LabTest.builder()
                    .code("HIV").name("HIV 1 & 2 Antibody")
                    .categoryId(immunology.getId())
                    .price(new BigDecimal("200")).turnaroundHours(4)
                    .unit(null).normalRange("Non-reactive").build());
            labTestRepository.save(LabTest.builder()
                    .code("HBS-AG").name("Hepatitis B Surface Antigen")
                    .categoryId(immunology.getId())
                    .price(new BigDecimal("250")).turnaroundHours(4)
                    .unit(null).normalRange("Non-reactive").build());
            labTestRepository.save(LabTest.builder()
                    .code("HCVAB").name("Hepatitis C Antibody")
                    .categoryId(immunology.getId())
                    .price(new BigDecimal("300")).turnaroundHours(4)
                    .unit(null).normalRange("Non-reactive").build());

            log.info("[DevSeeder] Pathology: 4 categories and 18 lab tests seeded.");
        } finally {
            TenantContext.clear();
        }
    }

    // ── Finance expense categories (hospital_001 schema) ──────────────────────

    private void seedFinanceData() {
        TenantContext.set(DEV_TENANT);
        try {
            if (expenseCategoryRepository.count() > 0) {
                log.info("[DevSeeder] Finance expense categories already exist — skipping.");
                return;
            }
            expenseCategoryRepository.save(ExpenseCategory.builder()
                    .name("Staff Salaries").description("Monthly salaries and wages").build());
            expenseCategoryRepository.save(ExpenseCategory.builder()
                    .name("Medicine Purchase").description("Drug and consumable procurement").build());
            expenseCategoryRepository.save(ExpenseCategory.builder()
                    .name("Equipment & Maintenance").description("Repair and upkeep of medical equipment").build());
            expenseCategoryRepository.save(ExpenseCategory.builder()
                    .name("Utilities").description("Electricity, water, internet, and phone bills").build());
            expenseCategoryRepository.save(ExpenseCategory.builder()
                    .name("Housekeeping & Sanitation").description("Cleaning supplies and laundry").build());
            expenseCategoryRepository.save(ExpenseCategory.builder()
                    .name("Catering & Patient Meals").description("Food services for admitted patients").build());
            expenseCategoryRepository.save(ExpenseCategory.builder()
                    .name("Administrative").description("Office supplies, printing, and stationery").build());
            expenseCategoryRepository.save(ExpenseCategory.builder()
                    .name("Miscellaneous").description("Other operational expenses").build());
            log.info("[DevSeeder] Finance: 8 expense categories seeded.");
        } finally {
            TenantContext.clear();
        }
    }

    // ── Inventory item categories & items (hospital_001 schema) ──────────────

    private void seedInventoryData() {
        TenantContext.set(DEV_TENANT);
        try {
            if (itemCategoryRepository.count() > 0) {
                log.info("[DevSeeder] Inventory data already exists — skipping.");
                return;
            }
            ItemCategory medical = itemCategoryRepository.save(
                    ItemCategory.builder().name("Medical Supplies")
                            .description("General medical consumables").build());
            ItemCategory surgical = itemCategoryRepository.save(
                    ItemCategory.builder().name("Surgical Consumables")
                            .description("Single-use surgical items").build());
            ItemCategory housekeeping = itemCategoryRepository.save(
                    ItemCategory.builder().name("Housekeeping")
                            .description("Cleaning and sanitation supplies").build());
            ItemCategory stationery = itemCategoryRepository.save(
                    ItemCategory.builder().name("Stationery")
                            .description("Office and administrative supplies").build());
            ItemCategory linen = itemCategoryRepository.save(
                    ItemCategory.builder().name("Linen & Bedding")
                            .description("Bed sheets, pillow covers, gowns").build());

            // Medical Supplies
            inventoryItemRepository.save(InventoryItem.builder()
                    .itemCode("MED-GLV-E").name("Examination Gloves (Box)")
                    .categoryId(medical.getId()).categoryName(medical.getName())
                    .unit("Box").reorderLevel(20).currentStock(50).build());
            inventoryItemRepository.save(InventoryItem.builder()
                    .itemCode("MED-MASK").name("Surgical Masks (Box)")
                    .categoryId(medical.getId()).categoryName(medical.getName())
                    .unit("Box").reorderLevel(30).currentStock(80).build());
            inventoryItemRepository.save(InventoryItem.builder()
                    .itemCode("MED-SAN").name("Hand Sanitizer (500ml)")
                    .categoryId(medical.getId()).categoryName(medical.getName())
                    .unit("Bottle").reorderLevel(15).currentStock(40).build());
            inventoryItemRepository.save(InventoryItem.builder()
                    .itemCode("MED-IV-CAN").name("IV Cannula (20G)")
                    .categoryId(medical.getId()).categoryName(medical.getName())
                    .unit("Piece").reorderLevel(50).currentStock(200).build());

            // Surgical Consumables
            inventoryItemRepository.save(InventoryItem.builder()
                    .itemCode("SUR-GLV-S").name("Surgical Gloves Size 7 (Pair)")
                    .categoryId(surgical.getId()).categoryName(surgical.getName())
                    .unit("Pair").reorderLevel(25).currentStock(60).build());
            inventoryItemRepository.save(InventoryItem.builder()
                    .itemCode("SUR-GAZE").name("Sterile Gauze (10x10cm)")
                    .categoryId(surgical.getId()).categoryName(surgical.getName())
                    .unit("Piece").reorderLevel(100).currentStock(500).build());
            inventoryItemRepository.save(InventoryItem.builder()
                    .itemCode("SUR-SYR-5").name("Syringe 5ml (Box of 100)")
                    .categoryId(surgical.getId()).categoryName(surgical.getName())
                    .unit("Box").reorderLevel(10).currentStock(8).build()); // Low stock

            // Housekeeping
            inventoryItemRepository.save(InventoryItem.builder()
                    .itemCode("HK-PHENYL").name("Phenyl Disinfectant (5L)")
                    .categoryId(housekeeping.getId()).categoryName(housekeeping.getName())
                    .unit("Can").reorderLevel(5).currentStock(12).build());
            inventoryItemRepository.save(InventoryItem.builder()
                    .itemCode("HK-TISSUE").name("Tissue Paper Rolls (Pack of 12)")
                    .categoryId(housekeeping.getId()).categoryName(housekeeping.getName())
                    .unit("Pack").reorderLevel(10).currentStock(3).build()); // Low stock

            // Stationery
            inventoryItemRepository.save(InventoryItem.builder()
                    .itemCode("STN-A4").name("A4 Paper Ream")
                    .categoryId(stationery.getId()).categoryName(stationery.getName())
                    .unit("Ream").reorderLevel(10).currentStock(25).build());
            inventoryItemRepository.save(InventoryItem.builder()
                    .itemCode("STN-PEN").name("Ball Point Pens (Box)")
                    .categoryId(stationery.getId()).categoryName(stationery.getName())
                    .unit("Box").reorderLevel(5).currentStock(8).build());

            // Linen
            inventoryItemRepository.save(InventoryItem.builder()
                    .itemCode("LIN-SHEET").name("Bed Sheet (Single)")
                    .categoryId(linen.getId()).categoryName(linen.getName())
                    .unit("Piece").reorderLevel(20).currentStock(45).build());
            inventoryItemRepository.save(InventoryItem.builder()
                    .itemCode("LIN-PILLOW").name("Pillow Cover")
                    .categoryId(linen.getId()).categoryName(linen.getName())
                    .unit("Piece").reorderLevel(20).currentStock(38).build());

            log.info("[DevSeeder] Inventory: 5 categories and 13 items seeded (3 in low stock).");
        } finally {
            TenantContext.clear();
        }
    }

    // ── Blood Bank seed data (hospital_001 schema) ────────────────────────────

    private void seedBloodBankData() {
        TenantContext.set(DEV_TENANT);
        try {
            if (bloodDonorRepository.count() > 0) {
                log.info("[DevSeeder] Blood bank data already exists — skipping.");
                return;
            }

            // Donors
            record DonorSeed(String first, String last, Gender gender,
                             String dob, BloodGroup bg, String mobile) {}
            var donors = java.util.List.of(
                    new DonorSeed("Arjun",   "Sharma",  Gender.MALE,   "1990-04-15", BloodGroup.O_POS,  "9800000001"),
                    new DonorSeed("Priya",   "Nair",    Gender.FEMALE, "1988-09-22", BloodGroup.A_POS,  "9800000002"),
                    new DonorSeed("Rahul",   "Verma",   Gender.MALE,   "1995-01-10", BloodGroup.B_POS,  "9800000003"),
                    new DonorSeed("Sunita",  "Patel",   Gender.FEMALE, "1992-07-30", BloodGroup.AB_POS, "9800000004"),
                    new DonorSeed("Karthik", "Menon",   Gender.MALE,   "1985-11-05", BloodGroup.O_NEG,  "9800000005"),
                    new DonorSeed("Deepa",   "Reddy",   Gender.FEMALE, "1993-03-18", BloodGroup.A_NEG,  "9800000006"),
                    new DonorSeed("Vijay",   "Kumar",   Gender.MALE,   "1991-06-25", BloodGroup.B_NEG,  "9800000007"),
                    new DonorSeed("Anita",   "Joshi",   Gender.FEMALE, "1987-12-02", BloodGroup.AB_NEG, "9800000008")
            );

            java.util.List<BloodDonor> saved = new java.util.ArrayList<>();
            int seq = 1;
            for (var d : donors) {
                BloodDonor donor = BloodDonor.builder()
                        .donorNumber(String.format("DON-%d-%05d", java.time.LocalDate.now().getYear(), seq++))
                        .firstName(d.first()).lastName(d.last())
                        .gender(d.gender())
                        .dateOfBirth(java.time.LocalDate.parse(d.dob()))
                        .bloodGroup(d.bg())
                        .mobile(d.mobile())
                        .build();
                saved.add(bloodDonorRepository.save(donor));
            }

            // Blood units — 2 AVAILABLE units per blood group + 2 PENDING_TESTING
            LocalDate today    = java.time.LocalDate.now();
            LocalDate expiry42 = today.plusDays(42);
            LocalDate expiry20 = today.plusDays(20);
            int unitSeq = 1;

            record UnitSeed(BloodGroup bg, java.time.LocalDate exp, UnitStatus st, TestingStatus ts, int donorIdx) {}
            var units = java.util.List.of(
                    new UnitSeed(BloodGroup.O_POS,  expiry42, UnitStatus.AVAILABLE,      TestingStatus.CLEARED,  0),
                    new UnitSeed(BloodGroup.O_POS,  expiry20, UnitStatus.AVAILABLE,      TestingStatus.CLEARED,  0),
                    new UnitSeed(BloodGroup.A_POS,  expiry42, UnitStatus.AVAILABLE,      TestingStatus.CLEARED,  1),
                    new UnitSeed(BloodGroup.A_POS,  expiry42, UnitStatus.AVAILABLE,      TestingStatus.CLEARED,  1),
                    new UnitSeed(BloodGroup.B_POS,  expiry42, UnitStatus.AVAILABLE,      TestingStatus.CLEARED,  2),
                    new UnitSeed(BloodGroup.B_POS,  expiry42, UnitStatus.AVAILABLE,      TestingStatus.CLEARED,  2),
                    new UnitSeed(BloodGroup.AB_POS, expiry42, UnitStatus.AVAILABLE,      TestingStatus.CLEARED,  3),
                    new UnitSeed(BloodGroup.AB_POS, expiry42, UnitStatus.AVAILABLE,      TestingStatus.CLEARED,  3),
                    new UnitSeed(BloodGroup.O_NEG,  expiry42, UnitStatus.AVAILABLE,      TestingStatus.CLEARED,  4),
                    new UnitSeed(BloodGroup.O_NEG,  expiry42, UnitStatus.AVAILABLE,      TestingStatus.CLEARED,  4),
                    new UnitSeed(BloodGroup.A_NEG,  expiry42, UnitStatus.AVAILABLE,      TestingStatus.CLEARED,  5),
                    new UnitSeed(BloodGroup.B_NEG,  expiry42, UnitStatus.AVAILABLE,      TestingStatus.CLEARED,  6),
                    new UnitSeed(BloodGroup.AB_NEG, expiry42, UnitStatus.AVAILABLE,      TestingStatus.CLEARED,  7),
                    // 2 pending testing
                    new UnitSeed(BloodGroup.O_POS,  expiry42, UnitStatus.PENDING_TESTING, TestingStatus.PENDING, 0),
                    new UnitSeed(BloodGroup.A_POS,  expiry42, UnitStatus.PENDING_TESTING, TestingStatus.PENDING, 1)
            );

            int yr = today.getYear();
            for (var u : units) {
                BloodDonor donor = saved.get(u.donorIdx());
                bloodUnitRepository.save(BloodUnit.builder()
                        .unitNumber(String.format("BU-%d-%05d", yr, unitSeq++))
                        .bloodGroup(u.bg())
                        .donorId(donor.getId())
                        .donorName(donor.getFirstName() + " " + donor.getLastName())
                        .componentType(ComponentType.WHOLE_BLOOD)
                        .volumeMl(450)
                        .collectionDate(today.minusDays(2))
                        .expiryDate(u.exp())
                        .testingStatus(u.ts())
                        .status(u.st())
                        .build());
            }

            log.info("[DevSeeder] Blood bank: 8 donors and 15 blood units seeded (13 AVAILABLE, 2 PENDING_TESTING).");
        } finally {
            TenantContext.clear();
        }
    }
}
