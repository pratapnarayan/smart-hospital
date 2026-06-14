package com.smarthospital.modules.setup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantCreateRequest(

        /** Human-readable hospital name, e.g. "City General Hospital" */
        @NotBlank @Size(max = 200)
        String name,

        /**
         * Optional explicit schema slug, e.g. "city_general".
         * If omitted the service derives one from the hospital name.
         * Must be lowercase letters, digits and underscores only (PostgreSQL schema rules).
         */
        @Pattern(regexp = "^[a-z][a-z0-9_]{1,61}[a-z0-9]$",
                 message = "Schema name must be 3-63 lowercase alphanumeric / underscore characters")
        String schemaSlug,

        /** Subscription plan: BASIC | STANDARD | ENTERPRISE */
        @Size(max = 50)
        String plan,

        /** First admin user email for this tenant */
        @NotBlank @Size(max = 150)
        String adminEmail,

        /** First admin user first name */
        @NotBlank @Size(max = 100)
        String adminFirstName,

        /** First admin user last name */
        @NotBlank @Size(max = 100)
        String adminLastName,

        /** Clinic operating mode: FULL_HMS | CLINIC_OPD (defaults to FULL_HMS if omitted) */
        @Pattern(regexp = "FULL_HMS|CLINIC_OPD",
                 message = "clinicType must be FULL_HMS or CLINIC_OPD")
        String clinicType
) {}
