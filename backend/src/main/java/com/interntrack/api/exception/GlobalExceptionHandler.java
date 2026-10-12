package com.interntrack.api.exception;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "error", "Validation Failed",
                "message", "Invalid request parameters",
                "details", errors
        ));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        Map<String, String> errors = new HashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                if (error instanceof FieldError fe) {
                    errors.put(fe.getField(), fe.getDefaultMessage());
                } else {
                    String fieldName = (result.getMethodParameter() != null && result.getMethodParameter().getParameterName() != null)
                            ? result.getMethodParameter().getParameterName()
                            : (error.getCodes() != null && error.getCodes().length > 0 ? error.getCodes()[0] : "field");
                    errors.put(fieldName, error.getDefaultMessage());
                }
            }
        }
        if (errors.isEmpty()) {
            for (MessageSourceResolvable error : ex.getAllErrors()) {
                if (error instanceof FieldError fe) {
                    errors.put(fe.getField(), fe.getDefaultMessage());
                } else {
                    String fieldName = error.getCodes() != null && error.getCodes().length > 0 ? error.getCodes()[0] : "field";
                    errors.put(fieldName, error.getDefaultMessage());
                }
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "error", "Validation Failed",
                "message", "Invalid request parameters",
                "details", errors
        ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "success", false,
                "error", "Unauthorized",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "success", false,
                "error", "Unauthorized",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "success", false,
                "error", "Unauthorized",
                "message", ex.getMessage() != null ? ex.getMessage() : "Authentication required"
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "error", "Forbidden",
                "message", "Access denied"
        ));
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<Map<String, Object>> handleAccountInactive(AccountInactiveException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "error", "Forbidden",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "success", false,
                "error", "Conflict",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(CompanyAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleCompanyAlreadyExists(CompanyAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "success", false,
                "error", "Conflict",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(OpportunityAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleOpportunityAlreadyExists(OpportunityAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "success", false,
                "error", "Conflict",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(DuplicateApplicationException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateApplication(DuplicateApplicationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "success", false,
                "error", "Conflict",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(DuplicateSkillException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateSkill(DuplicateSkillException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "success", false,
                "error", "Conflict",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "error", "Bad Request",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        if (rootMsg != null && (rootMsg.contains("uk_company_canonical") || rootMsg.toLowerCase().contains("canonical_name"))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "error", "Conflict",
                    "message", "Company with this canonical name already exists"
            ));
        }
        if (rootMsg != null && (rootMsg.contains("uk_app_user_opp") || (rootMsg.toLowerCase().contains("user_id") && rootMsg.toLowerCase().contains("opportunity_id")))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "error", "Conflict",
                    "message", "Application already exists for this opportunity"
            ));
        }
        if (rootMsg != null && (rootMsg.contains("uk_user_skill") || (rootMsg.toLowerCase().contains("user_id") && rootMsg.toLowerCase().contains("skill_id")))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "error", "Conflict",
                    "message", "Skill is already added to user profile"
            ));
        }
        if (rootMsg != null && (rootMsg.contains("uk_opp_skill") || (rootMsg.toLowerCase().contains("opportunity_id") && rootMsg.toLowerCase().contains("skill_id")))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "error", "Conflict",
                    "message", "Skill is already associated with this opportunity"
            ));
        }
        if (rootMsg != null && (rootMsg.contains("fk_app_opportunity") || rootMsg.toLowerCase().contains("opportunity_id"))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "error", "Conflict",
                    "message", "Cannot delete opportunity with existing candidate applications"
            ));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "success", false,
                "error", "Conflict",
                "message", "Database constraint violation occurred"
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "error", "Not Found",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "error", "Bad Request",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(org.springframework.http.HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(Map.of(
                        "success", false,
                        "error", "Too Many Requests",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Internal Server Error",
                "message", "An unexpected error occurred"
        ));
    }
}
