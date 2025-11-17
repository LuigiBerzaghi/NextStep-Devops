package com.softcode.nextstep.api.dto.journey;

import java.util.List;

public record JourneyHistoryResponse(
        List<JourneyHistoryItemResponse> journeys, int page, int size, long totalElements, int totalPages) {}
