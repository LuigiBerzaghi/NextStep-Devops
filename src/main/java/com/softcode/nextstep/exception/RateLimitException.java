package com.softcode.nextstep.exception;

import org.springframework.http.HttpStatus;

public class RateLimitException extends ApiException {

    public RateLimitException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT");
    }
}

