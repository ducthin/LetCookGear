package com.ducthin.LetCookGear.exception;

import com.ducthin.LetCookGear.dto.ApiResponse;
import com.ducthin.LetCookGear.logging.LogSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("API_ERR type=IllegalArgumentException method={} path={} message={}",
                request.getMethod(),
                                resolveMaskedPath(request),
                                LogSanitizer.sanitizeText(ex.getMessage()));
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("API_ERR type=MethodArgumentNotValidException method={} path={} message={}",
                request.getMethod(),
                                resolveMaskedPath(request),
                                LogSanitizer.sanitizeText(ex.getMessage()));
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (oldVal, newVal) -> oldVal));
        return ResponseEntity.badRequest().body(ApiResponse.error("Dữ liệu đầu vào không hợp lệ", errors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("API_ERR type=AccessDeniedException method={} path={} message={}",
                request.getMethod(),
                                resolveMaskedPath(request),
                                LogSanitizer.sanitizeText(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Bạn không có quyền truy cập", null));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("API_ERR type=BadCredentialsException method={} path={} message={}",
                request.getMethod(),
                                resolveMaskedPath(request),
                                LogSanitizer.sanitizeText(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Email hoặc mật khẩu không đúng", null));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.warn("API_ERR type=AuthenticationException method={} path={} message={}",
                request.getMethod(),
                                resolveMaskedPath(request),
                                LogSanitizer.sanitizeText(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Xác thực thất bại", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("API_ERR type=Exception method={} path={} message={}",
                request.getMethod(),
                resolveMaskedPath(request),
                LogSanitizer.sanitizeText(ex.getMessage()),
                ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Đã xảy ra lỗi hệ thống", null));
    }

    private String resolveMaskedPath(HttpServletRequest request) {
        String query = request.getQueryString();
        String path = query == null || query.isBlank()
                ? request.getRequestURI()
                : request.getRequestURI() + "?" + query;
        return LogSanitizer.sanitizeText(path);
    }
}
