package com.softcode.nextstep.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String messageKey, Object... messageArgs) {
        super(messageKey, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", null, messageArgs);
    }
}
