package com.softcode.nextstep.api.dto.common;

import java.util.Map;

public record ErrorResponse(String error, String message, Map<String, ?> details) {
}

