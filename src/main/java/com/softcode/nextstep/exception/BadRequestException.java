package com.softcode.nextstep.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String messageKey, Object... messageArgs) {
        super(messageKey, HttpStatus.BAD_REQUEST, "BAD_REQUEST", null, messageArgs);
    }

    public BadRequestException(String messageKey, Map<String, ?> details) {
        super(messageKey, HttpStatus.BAD_REQUEST, "BAD_REQUEST", details);
    }
}
