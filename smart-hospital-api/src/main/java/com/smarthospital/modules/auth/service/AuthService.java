package com.smarthospital.modules.auth.service;

import com.smarthospital.core.exception.ApiException;
import com.smarthospital.core.security.JwtTokenProvider;
import com.smarthospital.core.token.RefreshTokenStore;
import com.smarthospital.modules.auth.domain.Permission;
import com.smarthospital.modules.auth.domain.Role;
import com.smarthospital.modules.auth.domain.User;
import com.smarthospital.modules.auth.dto.LoginRequest;
import com.smarthospital.modules.auth.dto.LoginResponse;
import com.smarthospital.modules.auth.dto.TokenResponse;
import com.smarthospital.modules.auth.dto.UserResponse;
import com.smarthospital.modules.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.transaction.annotation.Propagation;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final JwtTokenProvider  jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final PasswordEncoder   passwordEncoder;
    private final UserRepository    userRepository;

    public AuthService(JwtTokenProvider  jwtTokenProvider,
                       RefreshTokenStore  refreshTokenStore,
                       PasswordEncoder    passwordEncoder,
                       UserRepository     userRepository) {
        this.jwtTokenProvider  = jwtTokenProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.passwordEncoder   = passwordEncoder;
        this.userRepository    = userRepository;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LoginResponse login(LoginRequest request) {
        // Set tenant context from the request if provided.
        // If absent, TenantIdentifierResolver defaults to "public" (super-admin path).
        // In production, TenantFilter resolves this from the JWT subdomain before login.
        if (org.springframework.util.StringUtils.hasText(request.tenantId())) {
            com.smarthospital.core.tenant.TenantContext.set(request.tenantId());
        }

        User user;
        try {
            user = userRepository.findByEmailIgnoreCase(request.email())
                    .orElseThrow(() -> ApiException.badRequest("INVALID_CREDENTIALS", "Invalid email or password"));
        } finally {
            // Always clear — the filter chain will re-set it from the JWT on subsequent requests
            com.smarthospital.core.tenant.TenantContext.clear();
        }

        if (!user.isActive()) {
            throw ApiException.forbidden("ACCOUNT_DISABLED", "This account has been disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.badRequest("INVALID_CREDENTIALS", "Invalid email or password");
        }

        List<String> roles       = List.of(user.getRole().name());
        List<String> permissions = permissionsFor(user.getRole());

        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getId().toString(), user.getTenantId(),
                user.getEmail(), roles, permissions);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString());

        refreshTokenStore.save(user.getId().toString(), refreshToken);

        log.info("User {} logged in (tenant: {})", user.getEmail(), user.getTenantId());

        return new LoginResponse(
                TokenResponse.of(accessToken, refreshToken, 900),
                UserResponse.from(user)
        );
    }

    public TokenResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw ApiException.badRequest("INVALID_TOKEN", "Refresh token is invalid or expired");
        }
        String userId = jwtTokenProvider.getUserId(refreshToken);
        String stored = refreshTokenStore.get(userId);

        if (!refreshToken.equals(stored)) {
            throw ApiException.badRequest("INVALID_TOKEN", "Refresh token has been revoked");
        }

        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));

        String newRefresh = jwtTokenProvider.generateRefreshToken(userId);
        refreshTokenStore.save(userId, newRefresh);

        List<String> roles       = List.of(user.getRole().name());
        List<String> permissions = permissionsFor(user.getRole());
        String newAccess = jwtTokenProvider.generateAccessToken(
                userId, user.getTenantId(), user.getEmail(), roles, permissions);

        return TokenResponse.of(newAccess, newRefresh, 900);
    }

    @Transactional
    public void logout(String userId) {
        refreshTokenStore.delete(userId);
        log.info("User {} logged out", userId);
    }

    // ---------- helpers ----------

    /**
     * Returns the default permission set for each role.
     * In Phase 2 this will be driven by a roles_permissions table.
     */
    private List<String> permissionsFor(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> List.of("*");
            case ADMIN       -> List.of(
                    Permission.PATIENT_VIEW, Permission.PATIENT_CREATE, Permission.PATIENT_EDIT,
                    Permission.OPD_VIEW, Permission.OPD_CREATE, Permission.OPD_EDIT,
                    Permission.IPD_VIEW, Permission.IPD_CREATE, Permission.IPD_EDIT, Permission.IPD_MANAGE,
                    Permission.HR_VIEW, Permission.HR_CREATE, Permission.HR_EDIT, Permission.HR_MANAGE,
                    Permission.FRONTOFFICE_VIEW, Permission.FRONTOFFICE_CREATE, Permission.FRONTOFFICE_EDIT,
                    Permission.PHARMACY_VIEW, Permission.PHARMACY_CREATE,
                    Permission.PATHOLOGY_VIEW, Permission.PATHOLOGY_CREATE,
                    Permission.PATHOLOGY_EDIT, Permission.PATHOLOGY_MANAGE,
                    Permission.RADIOLOGY_VIEW, Permission.RADIOLOGY_CREATE,
                    Permission.RADIOLOGY_EDIT, Permission.RADIOLOGY_MANAGE,
                    Permission.FINANCE_VIEW, Permission.FINANCE_CREATE, Permission.FINANCE_MANAGE,
                    Permission.INVENTORY_VIEW, Permission.INVENTORY_CREATE, Permission.INVENTORY_MANAGE,
                    Permission.BLOODBANK_VIEW, Permission.BLOODBANK_CREATE, Permission.BLOODBANK_EDIT,
                    Permission.OPERATION_VIEW, Permission.OPERATION_CREATE,
                    Permission.OPERATION_EDIT, Permission.OPERATION_MANAGE,
                    Permission.REPORTS_VIEW);
            case DOCTOR      -> List.of(
                    Permission.PATIENT_VIEW,
                    Permission.OPD_VIEW, Permission.OPD_CREATE, Permission.OPD_EDIT,
                    Permission.IPD_VIEW, Permission.IPD_CREATE, Permission.IPD_EDIT,
                    Permission.PATHOLOGY_VIEW, Permission.PATHOLOGY_CREATE,
                    Permission.RADIOLOGY_VIEW, Permission.RADIOLOGY_CREATE,
                    Permission.OPERATION_VIEW, Permission.OPERATION_CREATE, Permission.OPERATION_EDIT);
            case NURSE       -> List.of(
                    Permission.PATIENT_VIEW, Permission.OPD_VIEW,
                    Permission.PATHOLOGY_VIEW, Permission.RADIOLOGY_VIEW,
                    Permission.OPERATION_VIEW);
            case PHARMACIST  -> List.of(
                    Permission.PATIENT_VIEW, Permission.PHARMACY_VIEW, Permission.PHARMACY_CREATE);
            case RECEPTIONIST -> List.of(
                    Permission.PATIENT_VIEW, Permission.PATIENT_CREATE,
                    Permission.FRONTOFFICE_VIEW, Permission.FRONTOFFICE_CREATE, Permission.FRONTOFFICE_EDIT);
            case ACCOUNTANT  -> List.of(
                    Permission.FINANCE_VIEW, Permission.FINANCE_CREATE, Permission.FINANCE_MANAGE,
                    Permission.REPORTS_VIEW);
            case PATHOLOGIST -> List.of(
                    Permission.PATIENT_VIEW,
                    Permission.PATHOLOGY_VIEW, Permission.PATHOLOGY_CREATE,
                    Permission.PATHOLOGY_EDIT, Permission.PATHOLOGY_MANAGE);
            case RADIOLOGIST -> List.of(
                    Permission.PATIENT_VIEW,
                    Permission.RADIOLOGY_VIEW, Permission.RADIOLOGY_CREATE,
                    Permission.RADIOLOGY_EDIT, Permission.RADIOLOGY_MANAGE);
            case PATIENT     -> List.of(Permission.PATIENT_VIEW);
        };
    }
}
