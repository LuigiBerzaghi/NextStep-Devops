package com.softcode.nextstep.exception;

import org.springframework.http.HttpStatus;

public class RateLimitException extends ApiException {

    public RateLimitException(String messageKey, Object... messageArgs) {
        super(messageKey, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT", null, messageArgs);
    }
}
