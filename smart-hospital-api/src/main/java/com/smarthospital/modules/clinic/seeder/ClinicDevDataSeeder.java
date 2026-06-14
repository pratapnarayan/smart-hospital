package com.smarthospital.modules.clinic.seeder;

import com.smarthospital.core.tenant.TenantContext;
import com.smarthospital.core.tenant.TenantMigrationService;
import com.smarthospital.modules.auth.domain.Role;
import com.smarthospital.modules.auth.domain.User;
import com.smarthospital.modules.auth.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Seeds the clinic_001 demo tenant and its five demo users on every dev startup.
 *
 * Uses @PostConstruct, which fires during bean initialization — before any ApplicationRunner
 * beans execute. @Order has no effect here; it is intentionally omitted.
 * Spring Boot's Flyway auto-config runs V1 (public.tenants creation) before user beans
 * are initialized, so the INSERT below is safe on first run.
 *
 * Self-provisioning steps (idempotent):
 *   1. INSERT into public.tenants ON CONFLICT DO NOTHING
 *   2. CREATE SCHEMA clinic_001 via raw JDBC with autoCommit=true (DDL cannot be in a transaction)
 *   3. Run Flyway migrations inside clinic_001 via TenantMigrationService.migrateTenantSchema()
 *
 * NOTE: No @Transactional here — same reason as DevDataSeeder.
 * Without @Transactional, each userRepository.save() opens its own transaction and
 * gets a fresh connection from TenantAwareDataSource, which reads TenantContext at
 * that moment and sets search_path correctly.  With @Transactional the connection
 * would be acquired once (with TenantContext still null) and all saves would land
 * in the public schema.
 */
@Component
@Profile("dev")
public class ClinicDevDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(ClinicDevDataSeeder.class);

    private static final String TENANT_SCHEMA = "clinic_001";
    private static final String TENANT_NAME   = "Demo Clinic";
    private static final String CLINIC_TYPE   = "CLINIC_OPD";

    // ── Demo users ───────────────────────────────────────────────────────────────
    private static final String ADMIN_EMAIL        = "admin@clinic001.com";
    private static final String ADMIN_PASS         = "Admin@1234";

    private static final String DOCTOR_EMAIL       = "doctor@clinic001.com";
    private static final String DOCTOR_PASS        = "Doctor@1234";

    private static final String RECEPT_EMAIL       = "receptionist@clinic001.com";
    private static final String RECEPT_PASS        = "Recept@1234";

    private static final String PATHOLOGIST_EMAIL  = "pathologist@clinic001.com";
    private static final String PATHOLOGIST_PASS   = "Path@1234";

    private static final String TECHNICIAN_EMAIL   = "technician@clinic001.com";
    private static final String TECHNICIAN_PASS    = "Tech@1234";

    // ── Injected dependencies ────────────────────────────────────────────────────

    // DataSource here is the raw HikariCP pool — NOT TenantAwareDataSource.
    // TenantAwareDataSource is created inline in JpaConfig and is never a Spring bean,
    // so Spring always injects the underlying HikariCP DataSource directly.
    private final DataSource             dataSource;
    private final JdbcTemplate           jdbc;
    private final TenantMigrationService tenantMigrationService;
    private final UserRepository         userRepository;
    private final PasswordEncoder        passwordEncoder;

    public ClinicDevDataSeeder(DataSource             dataSource,
                               TenantMigrationService tenantMigrationService,
                               UserRepository         userRepository,
                               PasswordEncoder        passwordEncoder) {
        this.dataSource             = dataSource;
        this.jdbc                   = new JdbcTemplate(dataSource);
        this.tenantMigrationService = tenantMigrationService;
        this.userRepository         = userRepository;
        this.passwordEncoder        = passwordEncoder;
    }

    @PostConstruct
    public void seed() {
        try {
            provisionTenant();
        } catch (Exception e) {
            log.error("[ClinicSeeder] Tenant provisioning failed — {}", e.getMessage(), e);
            return;
        }

        seedUsers();
    }

    // ── Step 1-3: Tenant self-provisioning ───────────────────────────────────────

    private void provisionTenant() throws Exception {
        // Step 1 — register in public.tenants (idempotent)
        jdbc.update(
            "INSERT INTO public.tenants (name, schema_name, plan, status, clinic_type) " +
            "VALUES (?, ?, 'BASIC', 'ACTIVE', ?) " +
            "ON CONFLICT (schema_name) DO NOTHING",
            TENANT_NAME, TENANT_SCHEMA, CLINIC_TYPE);
        log.info("[ClinicSeeder] '{}' registered in public.tenants.", TENANT_SCHEMA);

        // Step 2 — create PostgreSQL schema (DDL must run outside any transaction)
        // The injected DataSource is the raw HikariCP pool so getConnection() returns
        // a plain JDBC connection with no search_path interference.
        try (Connection conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            conn.setAutoCommit(true);
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + TENANT_SCHEMA);
            log.info("[ClinicSeeder] PostgreSQL schema '{}' ensured.", TENANT_SCHEMA);
        }

        // Step 3 — run Flyway migrations inside the new schema
        tenantMigrationService.migrateTenantSchema(TENANT_SCHEMA);
    }

    // ── Demo user seeding ────────────────────────────────────────────────────────

    private void seedUsers() {
        TenantContext.set(TENANT_SCHEMA);
        try {
            seedUser(ADMIN_EMAIL,       ADMIN_PASS,       Role.ADMIN,             "Clinic",      "Admin");
            seedUser(DOCTOR_EMAIL,      DOCTOR_PASS,      Role.DOCTOR,            "Demo",        "Doctor");
            seedUser(RECEPT_EMAIL,      RECEPT_PASS,      Role.RECEPTIONIST,      "Demo",        "Receptionist");
            seedUser(PATHOLOGIST_EMAIL, PATHOLOGIST_PASS, Role.PATHOLOGIST,       "Demo",        "Pathologist");
            seedUser(TECHNICIAN_EMAIL,  TECHNICIAN_PASS,  Role.CLINIC_TECHNICIAN, "Demo",        "Technician");

            log.info("===========================================================");
            log.info("[ClinicSeeder] clinic_001 demo users ready.");
            log.info("  Tenant : {}", TENANT_SCHEMA);
            log.info("  {} (ADMIN)", ADMIN_EMAIL);
            log.info("  {} (DOCTOR)", DOCTOR_EMAIL);
            log.info("  {} (RECEPTIONIST)", RECEPT_EMAIL);
            log.info("  {} (PATHOLOGIST)", PATHOLOGIST_EMAIL);
            log.info("  {} (CLINIC_TECHNICIAN)", TECHNICIAN_EMAIL);
            log.info("===========================================================");
        } finally {
            TenantContext.clear();
        }
    }

    private void seedUser(String email, String rawPassword, Role role,
                          String firstName, String lastName) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.info("[ClinicSeeder] {} already exists — skipping.", email);
            return;
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .firstName(firstName)
                .lastName(lastName)
                .tenantId(TENANT_SCHEMA)
                .role(role)
                .active(true)
                .build();
        userRepository.save(user);
        log.info("[ClinicSeeder] Created {} ({}).", email, role);
    }
}
