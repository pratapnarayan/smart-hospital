package com.smarthospital.modules.setup.controller;

import com.smarthospital.core.tenant.TenantContext;
import com.smarthospital.core.tenant.TenantRepository;
import com.smarthospital.modules.setup.dto.TenantClinicConfigResponse;
import com.smarthospital.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenant Config")
public class TenantConfigController {

    private final TenantRepository tenantRepository;

    public TenantConfigController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @GetMapping("/current/config")
    @Operation(summary = "Get clinic configuration for the current tenant")
    public ResponseEntity<ApiResponse<TenantClinicConfigResponse>> getCurrentConfig() {
        String schemaName = TenantContext.get();
        if (schemaName == null || schemaName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return tenantRepository.findBySchemaName(schemaName)
                .map(t -> ResponseEntity.ok(ApiResponse.ok(new TenantClinicConfigResponse(t.getClinicType()))))
                .orElse(ResponseEntity.notFound().build());
    }
}
