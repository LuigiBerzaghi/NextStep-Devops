package com.softcode.nextstep.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softcode.nextstep.api.dto.common.CareerSuggestionDto;
import com.softcode.nextstep.api.dto.common.SkillDto;
import com.softcode.nextstep.api.dto.common.SuggestedPathDto;
import com.softcode.nextstep.api.dto.dashboard.DashboardNextStepDto;
import com.softcode.nextstep.api.dto.dashboard.DashboardResponse;
import com.softcode.nextstep.api.dto.dashboard.DashboardTrendDto;
import com.softcode.nextstep.api.dto.dashboard.DashboardUserDto;
import com.softcode.nextstep.domain.journey.Journey;
import com.softcode.nextstep.domain.journey.JourneyStatus;
import com.softcode.nextstep.domain.journey.JourneyStepStatus;
import com.softcode.nextstep.domain.resume.ResumeAnalysis;
import com.softcode.nextstep.domain.user.User;
import com.softcode.nextstep.exception.BadRequestException;
import com.softcode.nextstep.repository.JourneyRepository;
import com.softcode.nextstep.repository.ResumeAnalysisRepository;
import com.softcode.nextstep.security.AuthenticatedUserContext;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AuthenticatedUserContext authenticatedUserContext;
    private final JourneyRepository journeyRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "dashboard", key = "@authenticatedUserContext.getCurrentUser().getId()")
    public DashboardResponse getDashboard() {
        User user = authenticatedUserContext.getCurrentUser();
        Optional<Journey> journeyOpt = journeyRepository.findTopByUserAndStatusOrderByCreatedAtDesc(user, JourneyStatus.ACTIVE);
        ResumeAnalysis analysis = resumeAnalysisRepository
                .findTopByUserOrderByAnalyzedAtDesc(user)
                .orElseThrow(() -> new BadRequestException("error.dashboard.resume_required"));
        DashboardUserDto userDto = new DashboardUserDto(
                Optional.ofNullable(user.getName()).orElse("Novo talento"),
                user.getCurrentJob(),
                journeyOpt.map(Journey::getDesiredJob).orElse(null));
        DashboardNextStepDto nextStep = journeyOpt.map(this::toNextStep).orElse(null);
        List<SkillDto> skills =
                readJsonOrEmpty(analysis.getSkillsJson(), new TypeReference<List<SkillDto>>() {});
        List<DashboardTrendDto> trends = List.of(
                new DashboardTrendDto("IA Generativa", "bot"),
                new DashboardTrendDto("Web3", "zap"),
                new DashboardTrendDto("DevOps", "settings"),
                new DashboardTrendDto("Cloud Native", "cloud"));
        List<SuggestedPathDto> suggestedPaths = readJsonOrEmpty(
                        analysis.getSuggestedCareersJson(), new TypeReference<List<CareerSuggestionDto>>() {})
                .stream()
                .map(item -> new SuggestedPathDto(item.title(), item.match()))
                .toList();
        return new DashboardResponse(userDto, nextStep, skills, trends, suggestedPaths);
    }

    private DashboardNextStepDto toNextStep(Journey journey) {
        return journey.getSteps().stream()
                .filter(step -> step.getStatus() != JourneyStepStatus.COMPLETED)
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .findFirst()
                .map(step -> new DashboardNextStepDto(step.getTitle(), step.getObjective(), step.isProgress()))
                .orElse(null);
    }

    private <T> T readJsonOrEmpty(String content, TypeReference<T> typeReference) {
        try {
            if (content == null) {
                return objectMapper.readValue("[]", typeReference);
            }
            return objectMapper.readValue(content, typeReference);
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel converter dados complexos", ex);
        }
    }
}
