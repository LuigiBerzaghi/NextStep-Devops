package com.softcode.nextstep.api.controller;

import com.softcode.nextstep.api.dto.journey.JourneyGenerationRequest;
import com.softcode.nextstep.api.dto.journey.JourneyHistoryResponse;
import com.softcode.nextstep.api.dto.journey.JourneyProgressUpdateRequest;
import com.softcode.nextstep.api.dto.journey.JourneyResponse;
import com.softcode.nextstep.api.dto.journey.JourneyStepResponse;
import com.softcode.nextstep.service.JourneyService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/journeys")
@RequiredArgsConstructor
public class JourneyController {

    private final JourneyService journeyService;

    @PostMapping("/generate")
    public ResponseEntity<JourneyResponse> generate(@Valid @RequestBody JourneyGenerationRequest request) {
        JourneyResponse response = journeyService.generateJourney(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/active")
    public ResponseEntity<JourneyResponse> activeJourney() {
        return ResponseEntity.ok(journeyService.getActiveJourney());
    }

    @PatchMapping("/steps/{stepId}/progress")
    public ResponseEntity<JourneyStepResponse> updateProgress(
            @PathVariable UUID stepId, @Valid @RequestBody JourneyProgressUpdateRequest request) {
        return ResponseEntity.ok(journeyService.updateStep(stepId, request));
    }

    @GetMapping("/history")
    public ResponseEntity<JourneyHistoryResponse> history(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(journeyService.getHistory(page, size));
    }
}
