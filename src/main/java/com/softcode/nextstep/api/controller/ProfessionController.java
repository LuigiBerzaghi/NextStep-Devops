package com.softcode.nextstep.api.controller;

import com.softcode.nextstep.api.dto.profession.ProfessionSuggestionResponse;
import com.softcode.nextstep.service.ProfessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professions")
@RequiredArgsConstructor
public class ProfessionController {

    private final ProfessionService professionService;

    @GetMapping("/suggested")
    public ResponseEntity<ProfessionSuggestionResponse> suggestions(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(professionService.findSuggestions(search));
    }
}

