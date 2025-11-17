package com.softcode.nextstep.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softcode.nextstep.api.dto.common.InsightDto;
import com.softcode.nextstep.api.dto.journey.JourneyGenerationRequest;
import com.softcode.nextstep.api.dto.journey.JourneyHistoryItemResponse;
import com.softcode.nextstep.api.dto.journey.JourneyHistoryResponse;
import com.softcode.nextstep.api.dto.journey.JourneyProgressUpdateRequest;
import com.softcode.nextstep.api.dto.journey.JourneyResponse;
import com.softcode.nextstep.api.dto.journey.JourneyStepResponse;
import com.softcode.nextstep.domain.journey.Journey;
import com.softcode.nextstep.domain.journey.JourneyStatus;
import com.softcode.nextstep.domain.journey.JourneyStep;
import com.softcode.nextstep.domain.journey.JourneyStepStatus;
import com.softcode.nextstep.domain.user.User;
import com.softcode.nextstep.exception.BadRequestException;
import com.softcode.nextstep.exception.NotFoundException;
import com.softcode.nextstep.messaging.NotificationProducer;
import com.softcode.nextstep.repository.JourneyRepository;
import com.softcode.nextstep.repository.JourneyStepRepository;
import com.softcode.nextstep.security.AuthenticatedUserContext;
import com.softcode.nextstep.service.ai.GeminiService;
import com.softcode.nextstep.service.ai.GeminiService.JourneyPlan;
import com.softcode.nextstep.service.ai.GeminiService.JourneyStepPlan;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class JourneyService {

    private final JourneyRepository journeyRepository;
    private final JourneyStepRepository journeyStepRepository;
    private final GeminiService geminiService;
    private final AuthenticatedUserContext authenticatedUserContext;
    private final ObjectMapper objectMapper;
    private final NotificationProducer notificationProducer;

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "@authenticatedUserContext.getCurrentUser().getId()")
    public JourneyResponse generateJourney(JourneyGenerationRequest request) {
        User user = authenticatedUserContext.getCurrentUser();
        JourneyPlan plan = geminiService.generateJourneyPlan(user, request);
        Journey journey = new Journey();
        journey.setUser(user);
        journey.setDesiredJob(request.desiredJob());
        journey.setStatus(JourneyStatus.ACTIVE);
        journey.setEstimatedTime(plan.estimatedTime());
        journey.setOverallProgress(0);
        journey.setInsightsJson(writeJson(plan.insights()));
        plan.steps().forEach(stepPlan -> journey.getSteps().add(toEntity(journey, stepPlan)));
        Journey saved = journeyRepository.save(journey);
        notificationProducer.notifyJourneyGenerated(user.getId(), saved.getId(), saved.getDesiredJob());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public JourneyResponse getActiveJourney() {
        User user = authenticatedUserContext.getCurrentUser();
        Journey journey = journeyRepository
                .findTopByUserAndStatusOrderByCreatedAtDesc(user, JourneyStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("error.journey.active_not_found"));
        return mapToResponse(journey);
    }

    @Transactional(readOnly = true)
    public JourneyHistoryResponse getHistory(int page, int size) {
        User user = authenticatedUserContext.getCurrentUser();
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        Page<Journey> pageResult = journeyRepository.findByUserAndStatus(
                user,
                JourneyStatus.COMPLETED,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "completedAt")));
        List<JourneyHistoryItemResponse> items = pageResult.getContent().stream()
                .map(journey -> new JourneyHistoryItemResponse(
                        journey.getId(),
                        journey.getDesiredJob(),
                        journey.getCompletedAt(),
                        journey.getOverallProgress(),
                        journey.getSteps().size()))
                .collect(Collectors.toList());
        return new JourneyHistoryResponse(
                items, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements(), pageResult.getTotalPages());
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "@authenticatedUserContext.getCurrentUser().getId()")
    public JourneyStepResponse updateStep(UUID stepId, JourneyProgressUpdateRequest request) {
        User user = authenticatedUserContext.getCurrentUser();
        JourneyStep step = journeyStepRepository
                .findByIdAndUser(stepId, user)
                .orElseThrow(() -> new NotFoundException("error.journey.step_not_found"));
        Journey journey = step.getJourney();
        if (journey.getStatus() != JourneyStatus.ACTIVE) {
            throw new BadRequestException("error.journey.cannot_update_completed");
        }
        step.setProgress(request.progress());
        step.setStatus(resolveStatus(request.progress()));
        step.setLastUpdate(LocalDateTime.now());
        journeyStepRepository.save(step);
        recalculateJourneyProgress(journey);
        journeyRepository.save(journey);
        return mapStep(step);
    }

    private JourneyStep toEntity(Journey journey, JourneyStepPlan plan) {
        JourneyStep step = new JourneyStep();
        step.setJourney(journey);
        step.setOrderIndex(plan.order());
        step.setTitle(plan.title());
        step.setObjective(plan.objective());
        step.setResources(plan.resources());
        step.setPlatformsJson(writeJson(sanitizePlatforms(plan.platforms())));
        step.setEstimatedTime(plan.estimatedTime());
        step.setProgress(0);
        step.setStatus(JourneyStepStatus.PENDING);
        step.setLastUpdate(LocalDateTime.now());
        return step;
    }

    private void recalculateJourneyProgress(Journey journey) {
        int total = journey.getSteps().size();
        int sum = journey.getSteps().stream().mapToInt(JourneyStep::getProgress).sum();
        int overall = total == 0 ? 0 : sum / total;
        journey.setOverallProgress(overall);
        boolean finished = journey.getSteps().stream().allMatch(step -> step.getStatus() == JourneyStepStatus.COMPLETED);
        if (finished) {
            journey.setStatus(JourneyStatus.COMPLETED);
            journey.setCompletedAt(LocalDateTime.now());
        }
    }

    private JourneyStepStatus resolveStatus(int progress) {
        if (progress <= 0) {
            return JourneyStepStatus.PENDING;
        }
        if (progress >= 100) {
            return JourneyStepStatus.COMPLETED;
        }
        return JourneyStepStatus.IN_PROGRESS;
    }

    private JourneyResponse mapToResponse(Journey journey) {
        List<JourneyStepResponse> steps = journey.getSteps().stream()
                .sorted(Comparator.comparingInt(JourneyStep::getOrderIndex))
                .map(this::mapStep)
                .toList();
        JourneyStepResponse nextStep = steps.stream().filter(step -> step.progress() < 100).findFirst().orElse(null);
        List<InsightDto> insights = readJson(journey.getInsightsJson(), new TypeReference<List<InsightDto>>() {});
        long completedSteps = steps.stream().filter(step -> "COMPLETED".equals(step.status())).count();
        return new JourneyResponse(
                journey.getId(),
                journey.getDesiredJob(),
                steps.size(),
                (int) completedSteps,
                journey.getEstimatedTime(),
                journey.getOverallProgress(),
                journey.getStatus().name().toLowerCase(),
                nextStep,
                steps,
                insights,
                journey.getCreatedAt(),
                journey.getUpdatedAt());
    }

    private JourneyStepResponse mapStep(JourneyStep step) {
        List<String> platforms =
                sanitizePlatforms(readJson(step.getPlatformsJson(), new TypeReference<List<String>>() {}));
        return new JourneyStepResponse(
                step.getId(),
                step.getOrderIndex(),
                step.getTitle(),
                step.getObjective(),
                step.getResources(),
                platforms,
                step.getEstimatedTime(),
                step.getProgress(),
                step.getStatus().name().toLowerCase(),
                step.getLastUpdate());
    }

    private List<String> sanitizePlatforms(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .map(value -> value == null ? "" : value.trim())
                .map(value -> {
                    if (!StringUtils.hasText(value)) {
                        return "";
                    }
                    String normalized = sanitizePlatformName(value);
                    String[] words = normalized.split("\\s+");
                    int limit = Math.min(words.length, 3);
                    return String.join(" ", Arrays.copyOf(words, limit));
                })
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String sanitizePlatformName(String value) {
        String stripped = value.replaceAll("<[^>]+>", "");
        stripped = stripped.replaceAll("\\(.+?\\)", "");
        stripped = stripped.replace("/", " ");
        stripped = stripped.replace("-", " ");
        return stripped.replaceAll("\\s{2,}", " ").trim();
    }

    private <T> T readJson(String content, TypeReference<T> typeReference) {
        try {
            if (content == null) {
                return objectMapper.readValue("[]", typeReference);
            }
            return objectMapper.readValue(content, typeReference);
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel converter dados de jornada", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao converter insights em JSON", e);
        }
    }
}
