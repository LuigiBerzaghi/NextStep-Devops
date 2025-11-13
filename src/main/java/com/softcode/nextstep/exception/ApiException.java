package com.softcode.nextstep.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final Map<String, ?> details;

    protected ApiException(String message, HttpStatus status, String errorCode) {
        this(message, status, errorCode, null);
    }

    protected ApiException(String message, HttpStatus status, String errorCode, Map<String, ?> details) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, ?> getDetails() {
        return details;
    }
}

