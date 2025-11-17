package com.softcode.nextstep.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softcode.nextstep.api.dto.common.CareerSuggestionDto;
import com.softcode.nextstep.api.dto.common.SkillDto;
import com.softcode.nextstep.api.dto.resume.ResumeAnalysisResponse;
import com.softcode.nextstep.api.dto.resume.ResumeSummaryDto;
import com.softcode.nextstep.domain.resume.ResumeAnalysis;
import com.softcode.nextstep.domain.user.User;
import com.softcode.nextstep.exception.BadRequestException;
import com.softcode.nextstep.exception.NotFoundException;
import com.softcode.nextstep.messaging.NotificationProducer;
import com.softcode.nextstep.repository.ResumeAnalysisRepository;
import com.softcode.nextstep.security.AuthenticatedUserContext;
import com.softcode.nextstep.service.ai.GeminiService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final List<String> SUPPORTED_EXTENSIONS = List.of("pdf", "doc", "docx");

    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final GeminiService geminiService;
    private final AuthenticatedUserContext authenticatedUserContext;
    private final ObjectMapper objectMapper;
    private final NotificationProducer notificationProducer;

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "@authenticatedUserContext.getCurrentUser().getId()")
    public ResumeAnalysisResponse uploadAndAnalyze(MultipartFile file) {
        User user = authenticatedUserContext.getCurrentUser();
        validateFile(file);
        ResumeSummaryDto summary;
        try {
            summary = geminiService.analyzeResume(user, file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("error.resume.file_read_failed");
        }
        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setUser(user);
        writeSummaryToEntity(summary, analysis);
        analysis.setExperienceLevel(summary.experienceLevel());
        analysis.setCurrentJob(summary.currentJob());
        analysis.setYearsOfExperience(summary.yearsOfExperience());
        analysis.setAnalyzedAt(LocalDateTime.now());
        resumeAnalysisRepository.save(analysis);
        notificationProducer.notifyResumeAnalyzed(user.getId(), analysis.getId());
        return mapToResponse(analysis);
    }

    public ResumeAnalysisResponse getLatestAnalysis() {
        User user = authenticatedUserContext.getCurrentUser();
        ResumeAnalysis analysis = resumeAnalysisRepository
                .findTopByUserOrderByAnalyzedAtDesc(user)
                .orElseThrow(() -> new NotFoundException("error.resume.analysis_not_found"));
        return mapToResponse(analysis);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("error.resume.file_required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("error.resume.file_too_large");
        }
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)) {
            throw new BadRequestException("error.resume.invalid_filename");
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) {
            throw new BadRequestException("error.resume.invalid_format");
        }
        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("error.resume.invalid_format");
        }
    }

    private ResumeAnalysisResponse mapToResponse(ResumeAnalysis analysis) {
        ResumeSummaryDto summary = new ResumeSummaryDto(
                readJson(analysis.getSkillsJson(), new TypeReference<List<SkillDto>>() {}),
                analysis.getExperienceLevel(),
                analysis.getCurrentJob(),
                analysis.getYearsOfExperience() == null ? 0 : analysis.getYearsOfExperience(),
                readJson(analysis.getGapsJson(), new TypeReference<List<String>>() {}),
                readJson(analysis.getSuggestedCareersJson(), new TypeReference<List<CareerSuggestionDto>>() {}));
        return new ResumeAnalysisResponse(analysis.getId(), summary, analysis.getAnalyzedAt());
    }

    private void writeSummaryToEntity(ResumeSummaryDto summary, ResumeAnalysis analysis) {
        analysis.setSkillsJson(writeJson(summary.currentSkills()));
        analysis.setGapsJson(writeJson(summary.gaps()));
        analysis.setSuggestedCareersJson(writeJson(summary.suggestedCareers()));
        analysis.setSummary("Analise automatizada do curriculo");
    }

    private <T> T readJson(String content, TypeReference<T> typeReference) {
        try {
            if (content == null) {
                return objectMapper.readValue("[]", typeReference);
            }
            return objectMapper.readValue(content, typeReference);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler dados armazenados", e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao persistir dados complexos", e);
        }
    }
}
