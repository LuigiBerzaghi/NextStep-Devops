package com.softcode.nextstep.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public BadRequestException(String message, Map<String, ?> details) {
        super(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST", details);
    }
}

