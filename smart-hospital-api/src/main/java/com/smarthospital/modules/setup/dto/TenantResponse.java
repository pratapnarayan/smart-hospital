package com.smarthospital.modules.setup.dto;

import com.smarthospital.core.tenant.Tenant;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID    id,
        String  name,
        String  schemaName,
        String  plan,
        String  status,
        Instant createdAt,
        String  clinicType
) {
    public static TenantResponse from(Tenant t) {
        return new TenantResponse(
                t.getId(), t.getName(), t.getSchemaName(),
                t.getPlan(), t.getStatus(), t.getCreatedAt(),
                t.getClinicType());
    }
}
