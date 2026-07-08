package com.histar.be.common.exception;

import com.histar.be.common.response.ErrorResponse;
import com.histar.be.common.response.ValidationErrorResponse;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        HttpStatus status = mapStatus(ex.getErrorCode());
        if (ex instanceof QuotaExceededException quotaEx) {
            return ResponseEntity.status(status)
                    .body(ErrorResponse.ofQuota(
                            ex.getErrorCode(),
                            ex.getMessage(),
                            quotaEx.getUpgradeUrl(),
                            quotaEx.getQuotaType(),
                            quotaEx.getUpgradePackage()));
        }
        return ResponseEntity.status(status).body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Invalid" : error.getDefaultMessage(),
                        (first, second) -> first));
        ValidationErrorResponse body = ValidationErrorResponse.builder()
                .code(ErrorCode.VALIDATION_ERROR.getCode())
                .message("Du lieu khong hop le")
                .fieldErrors(fieldErrors)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (first, second) -> first));
        ValidationErrorResponse body = ValidationErrorResponse.builder()
                .code(ErrorCode.VALIDATION_ERROR.getCode())
                .message("Du lieu khong hop le")
                .fieldErrors(fieldErrors)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ValidationErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        ValidationErrorResponse body = ValidationErrorResponse.builder()
                .code(ErrorCode.VALIDATION_ERROR.getCode())
                .message("Du lieu khong hop le")
                .fieldErrors(Map.of(ex.getParameterName(), "Tham so bat buoc bi thieu"))
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(ErrorCode.FORBIDDEN, "Forbidden"));
    }

    /** Sai email/mật khẩu khi đăng nhập (BadCredentials...) phải trả 401, không phải 500. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"));
    }

    @ExceptionHandler(CcuLimitException.class)
    public ResponseEntity<ErrorResponse> handleCcuLimit(CcuLimitException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.builder()
                        .code("CCU_LIMIT_EXCEEDED")
                        .message(ex.getMessage())
                        .type("ORG_CCU")
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Internal server error"));
    }

    private HttpStatus mapStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case CONFLICT -> HttpStatus.CONFLICT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case VALIDATION_ERROR -> HttpStatus.UNPROCESSABLE_ENTITY;
            case BUSINESS_RULE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case EMAIL_NOT_VERIFIED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case LMS_PREMIUM_REQUIRED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case QUOTA_EXCEEDED -> HttpStatus.FORBIDDEN;
            case TRIAL_EXPIRED -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
