package com.softcode.nextstep.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }
}

