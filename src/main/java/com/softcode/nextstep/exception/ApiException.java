package com.softcode.nextstep.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final Map<String, ?> details;
    private final Object[] messageArgs;

    protected ApiException(String messageKey, HttpStatus status, String errorCode) {
        this(messageKey, status, errorCode, null, (Object[]) null);
    }

    protected ApiException(String messageKey, HttpStatus status, String errorCode, Map<String, ?> details) {
        this(messageKey, status, errorCode, details, (Object[]) null);
    }

    protected ApiException(
            String messageKey, HttpStatus status, String errorCode, Map<String, ?> details, Object... messageArgs) {
        super(messageKey);
        this.status = status;
        this.errorCode = errorCode;
        this.details = details;
        this.messageArgs = messageArgs == null ? new Object[0] : messageArgs;
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

    public String getMessageKey() {
        return super.getMessage();
    }

    public Object[] getMessageArgs() {
        return messageArgs;
    }
}
