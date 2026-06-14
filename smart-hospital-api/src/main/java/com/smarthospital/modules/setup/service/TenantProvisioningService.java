package com.smarthospital.modules.setup.service;

import com.smarthospital.core.exception.ApiException;
import com.smarthospital.core.tenant.Tenant;
import com.smarthospital.core.tenant.TenantContext;
import com.smarthospital.core.tenant.TenantMigrationService;
import com.smarthospital.core.tenant.TenantRepository;
import com.smarthospital.modules.auth.domain.Role;
import com.smarthospital.modules.auth.domain.User;
import com.smarthospital.modules.auth.repository.UserRepository;
import com.smarthospital.modules.setup.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class TenantProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(TenantProvisioningService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TenantRepository       tenantRepository;
    private final TenantMigrationService migrationService;
    private final UserRepository         userRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JdbcTemplate           jdbcTemplate;
    private final DataSource             dataSource;

    public TenantProvisioningService(TenantRepository       tenantRepository,
                                     TenantMigrationService  migrationService,
                                     UserRepository          userRepository,
                                     PasswordEncoder         passwordEncoder,
                                     JdbcTemplate            jdbcTemplate,
                                     DataSource              dataSource) {
        this.tenantRepository  = tenantRepository;
        this.migrationService  = migrationService;
        this.userRepository    = userRepository;
        this.passwordEncoder   = passwordEncoder;
        this.jdbcTemplate      = jdbcTemplate;
        this.dataSource        = dataSource;
    }

    // ── Provisioning ────────────────────────────────────────────────────────────

    /**
     * Full tenant onboarding — NOT @Transactional on purpose.
     *
     * Why: CREATE SCHEMA inside an uncommitted Spring transaction causes a DDL
     * lock. Flyway then blocks on a second connection waiting for the schema to
     * be visible → deadlock.  Instead, each step commits immediately and the
     * catch block performs a compensating delete of the registry row on failure.
     *
     * Steps:
     *  1. Validate uniqueness
     *  2. INSERT into public.tenants  (committed via saveTenantRecord)
     *  3. CREATE SCHEMA               (DDL, auto-commit, outside any transaction)
     *  4. Flyway migrations           (Flyway manages its own connections)
     *  5. Seed first admin user       (committed via seedAdminUser)
     *  6. Return credentials
     */
    public TenantProvisionedResponse provision(TenantCreateRequest req) {
        String schemaName = resolveSchemaName(req);

        // ── Guard: uniqueness ────────────────────────────────────────────────
        if (tenantRepository.existsByNameIgnoreCase(req.name())) {
            throw ApiException.conflict("DUPLICATE_TENANT_NAME",
                    "A tenant named '" + req.name() + "' already exists");
        }
        if (tenantRepository.existsBySchemaName(schemaName)) {
            throw ApiException.conflict("DUPLICATE_SCHEMA",
                    "Schema '" + schemaName + "' is already taken");
        }

        // ── Step 1: Commit tenant registry row immediately ───────────────────
        // saveTenantRecord() is @Transactional(REQUIRES_NEW) — commits before we
        // call CREATE SCHEMA so there is no open transaction holding a DDL lock.
        Tenant tenant = saveTenantRecord(req, schemaName);
        log.info("[Provisioning] Tenant '{}' registered — schema: {}", tenant.getName(), schemaName);

        try {
            // ── Step 2: Create PostgreSQL schema (DDL, auto-commit) ───────────
            createSchema(schemaName);

            // ── Step 3: Run Flyway migrations — Flyway owns its connections ───
            migrationService.migrateTenantSchema(schemaName);

            // ── Step 4: Seed first admin user (new transaction) ───────────────
            String temporaryPassword = generateTemporaryPassword();
            seedAdminUser(req, schemaName, temporaryPassword);

            log.info("[Provisioning] Tenant '{}' fully provisioned.", tenant.getName());

            return new TenantProvisionedResponse(
                    TenantResponse.from(tenant),
                    req.adminEmail(),
                    temporaryPassword,
                    "http://localhost:8080/api/v1/auth/login",
                    "Tenant provisioned successfully. Share the temporary password securely — it is shown only once."
            );

        } catch (Exception e) {
            // Compensating action: remove registry row so the slot is free to retry
            log.error("[Provisioning] Failed for '{}' — removing registry row: {}",
                    schemaName, e.getMessage(), e);
            deleteTenantRecord(tenant);
            throw new RuntimeException("Tenant provisioning failed: " + e.getMessage(), e);
        }
    }

    /** Saves the tenant row in its own transaction that commits immediately. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Tenant saveTenantRecord(TenantCreateRequest req, String schemaName) {
        return tenantRepository.save(
                Tenant.builder()
                        .name(req.name())
                        .schemaName(schemaName)
                        .plan(StringUtils.hasText(req.plan()) ? req.plan().toUpperCase() : "BASIC")
                        .status("ACTIVE")
                        .clinicType(req.clinicType() != null ? req.clinicType() : "FULL_HMS")
                        .build());
    }

    /** Deletes the registry row in its own transaction (compensating action). */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void deleteTenantRecord(Tenant tenant) {
        tenantRepository.delete(tenant);
    }

    // ── Management ───────────────────────────────────────────────────────────────

    public List<TenantResponse> listTenants() {
        return tenantRepository.findAll().stream()
                .map(TenantResponse::from).toList();
    }

    public TenantResponse getTenant(UUID id) {
        return TenantResponse.from(findOrThrow(id));
    }

    @Transactional
    public TenantResponse updateTenant(UUID id, TenantUpdateRequest req) {
        Tenant tenant = findOrThrow(id);
        if (StringUtils.hasText(req.name()))   tenant.setName(req.name());
        if (StringUtils.hasText(req.plan()))   tenant.setPlan(req.plan().toUpperCase());
        if (StringUtils.hasText(req.status())) {
            validateStatus(req.status());
            tenant.setStatus(req.status().toUpperCase());
        }
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    @Transactional
    public void suspendTenant(UUID id) {
        Tenant tenant = findOrThrow(id);
        if ("SUSPENDED".equals(tenant.getStatus())) {
            throw ApiException.badRequest("ALREADY_SUSPENDED", "Tenant is already suspended");
        }
        tenant.setStatus("SUSPENDED");
        tenantRepository.save(tenant);
        log.info("[Provisioning] Tenant '{}' suspended", tenant.getName());
    }

    @Transactional
    public void reactivateTenant(UUID id) {
        Tenant tenant = findOrThrow(id);
        if ("ACTIVE".equals(tenant.getStatus())) {
            throw ApiException.badRequest("ALREADY_ACTIVE", "Tenant is already active");
        }
        tenant.setStatus("ACTIVE");
        tenantRepository.save(tenant);
        log.info("[Provisioning] Tenant '{}' reactivated", tenant.getName());
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    /**
     * Derives a safe PostgreSQL schema name from the hospital name.
     * e.g. "City General Hospital" → "city_general_hospital"
     * Explicit schemaSlug from the request takes precedence.
     */
    private String resolveSchemaName(TenantCreateRequest req) {
        if (StringUtils.hasText(req.schemaSlug())) {
            return req.schemaSlug().toLowerCase();
        }
        // Slugify: lowercase, replace non-alphanumeric runs with underscore, trim
        String slug = req.name()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");   // strip leading/trailing underscores

        // Ensure it starts with a letter (PostgreSQL requirement)
        if (slug.isEmpty() || !Character.isLetter(slug.charAt(0))) {
            slug = "tenant_" + slug;
        }
        // Cap at 63 chars
        if (slug.length() > 63) slug = slug.substring(0, 63);

        // If already taken, append a short random suffix
        String candidate = slug;
        int attempt = 0;
        while (tenantRepository.existsBySchemaName(candidate)) {
            candidate = slug.substring(0, Math.min(slug.length(), 56)) + "_" + (++attempt);
        }
        return candidate;
    }

    /**
     * Creates the PostgreSQL schema using a dedicated auto-commit connection.
     * Using a fresh connection (not the Spring-managed transaction connection)
     * ensures the DDL commits immediately and is visible to Flyway.
     */
    private void createSchema(String schemaName) {
        if (!schemaName.matches("^[a-z][a-z0-9_]{0,62}$")) {
            throw new IllegalArgumentException("Unsafe schema name: " + schemaName);
        }
        try (java.sql.Connection conn = dataSource.getConnection()) {
            boolean prev = conn.getAutoCommit();
            conn.setAutoCommit(true);
            try (java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
            } finally {
                conn.setAutoCommit(prev);
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to create schema: " + schemaName, e);
        }
        log.info("[Provisioning] Schema '{}' created", schemaName);
    }

    /** Creates the first ADMIN user inside the newly provisioned tenant schema. */
    private void seedAdminUser(TenantCreateRequest req, String schemaName, String temporaryPassword) {
        TenantContext.set(schemaName);
        try {
            if (userRepository.existsByEmailIgnoreCase(req.adminEmail())) {
                // Shouldn't happen for a new schema, but guard anyway
                log.warn("[Provisioning] Admin user {} already exists in {}", req.adminEmail(), schemaName);
                return;
            }
            User admin = User.builder()
                    .email(req.adminEmail())
                    .passwordHash(passwordEncoder.encode(temporaryPassword))
                    .firstName(req.adminFirstName())
                    .lastName(req.adminLastName())
                    .tenantId(schemaName)
                    .role(Role.ADMIN)
                    .active(true)
                    .build();
            userRepository.save(admin);
            log.info("[Provisioning] Admin user {} seeded in schema {}", req.adminEmail(), schemaName);
        } finally {
            TenantContext.clear();
        }
    }

    /** Generates a cryptographically random 12-char temporary password. */
    private String generateTemporaryPassword() {
        byte[] bytes = new byte[9];   // 9 bytes → 12 base64 chars
        RANDOM.nextBytes(bytes);
        // Replace URL-unsafe chars with safe alphanumeric equivalents
        return Base64.getUrlEncoder().withoutPadding()
                     .encodeToString(bytes)
                     .replace('-', 'A')
                     .replace('_', 'z');
    }

    private Tenant findOrThrow(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("TENANT_NOT_FOUND",
                        "Tenant " + id + " not found"));
    }

    private void validateStatus(String status) {
        if (!List.of("ACTIVE", "SUSPENDED", "INACTIVE").contains(status.toUpperCase())) {
            throw ApiException.badRequest("INVALID_STATUS",
                    "Status must be ACTIVE, SUSPENDED or INACTIVE");
        }
    }
}
