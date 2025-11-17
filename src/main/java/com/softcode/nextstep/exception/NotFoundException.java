package com.softcode.nextstep.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String messageKey, Object... messageArgs) {
        super(messageKey, HttpStatus.NOT_FOUND, "NOT_FOUND", null, messageArgs);
    }
}
