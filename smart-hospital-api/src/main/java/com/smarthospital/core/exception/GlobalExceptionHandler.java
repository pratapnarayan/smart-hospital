package com.smarthospital.core.exception;

import com.smarthospital.shared.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        ErrorResponse error = ErrorResponse.builder()
                .code("VALIDATION_FAILED")
                .message("Request validation failed")
                .details(details)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> details = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (a, b) -> a));
        ErrorResponse error = ErrorResponse.builder()
                .code("VALIDATION_FAILED")
                .message("Constraint violation")
                .details(details)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .code("INVALID_REQUEST_BODY")
                .message("Request body is malformed or contains an invalid value")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = extractConstraintMessage(ex);
        ErrorResponse error = ErrorResponse.builder()
                .code("DATA_CONFLICT")
                .message(message)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception ex) {
        ErrorResponse error = ErrorResponse.builder()
                .code("NOT_FOUND")
                .message("The requested resource was not found")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName();
        String type = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        ErrorResponse error = ErrorResponse.builder()
                .code("INVALID_PARAMETER")
                .message("Parameter '" + name + "' must be a valid " + type)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .code("MISSING_PARAMETER")
                .message("Required parameter '" + ex.getParameterName() + "' is missing")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .code("ACCESS_DENIED")
                .message("You do not have permission to perform this action")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorResponse error = ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.internalServerError().body(ApiResponse.error(error));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String extractConstraintMessage(DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause().getMessage();
        if (cause == null) return "A duplicate or invalid record was detected";
        // PostgreSQL unique-violation messages contain "Key (column)=(value) already exists"
        if (cause.contains("already exists")) {
            int keyStart = cause.indexOf("Key (");
            if (keyStart >= 0) {
                return "Duplicate value: " + cause.substring(keyStart);
            }
            return "A record with this value already exists";
        }
        // FK violation: "is not present in table"
        if (cause.contains("is not present in table")) {
            return "Referenced record does not exist";
        }
        // NOT NULL violation
        if (cause.contains("null value in column")) {
            return "A required field is missing";
        }
        return "A data integrity constraint was violated";
    }
}
