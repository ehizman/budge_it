package com.relab.budgetpro.shared.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final Object details;

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = null;
    }

    public BusinessException(String code, String message, HttpStatus status, Object details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }

    // ── Convenience factories ─────────────────────────────────────

    public static BusinessException notFound(String resource) {
        return new BusinessException("NOT_FOUND", resource + " not found", HttpStatus.NOT_FOUND);
    }

    public static BusinessException conflict(String code, String message) {
        return new BusinessException(code, message, HttpStatus.CONFLICT);
    }

    public static BusinessException conflict(String code, String message, Object details) {
        return new BusinessException(code, message, HttpStatus.CONFLICT, details);
    }

    public static BusinessException unprocessable(String code, String message) {
        return new BusinessException(code, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static BusinessException forbidden(String code, String message) {
        return new BusinessException(code, message, HttpStatus.FORBIDDEN);
    }
}
