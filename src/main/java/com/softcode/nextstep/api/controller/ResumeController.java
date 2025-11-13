package com.softcode.nextstep.api.controller;

import com.softcode.nextstep.api.dto.resume.ResumeAnalysisResponse;
import com.softcode.nextstep.exception.UnauthorizedException;
import com.softcode.nextstep.security.AuthenticatedUserContext;
import com.softcode.nextstep.service.ResumeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final AuthenticatedUserContext authenticatedUserContext;

    @PostMapping("/upload")
    public ResponseEntity<ResumeAnalysisResponse> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(resumeService.uploadAndAnalyze(file));
    }

    @GetMapping("/analysis/{userId}")
    public ResponseEntity<ResumeAnalysisResponse> findLatest(@PathVariable UUID userId) {
        if (!authenticatedUserContext.getCurrentUser().getId().equals(userId)) {
            throw new UnauthorizedException("Voce so pode acessar seus proprios relatorios");
        }
        return ResponseEntity.ok(resumeService.getLatestAnalysis());
    }
}
