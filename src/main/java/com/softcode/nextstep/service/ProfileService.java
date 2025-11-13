package com.softcode.nextstep.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softcode.nextstep.api.dto.common.SkillDto;
import com.softcode.nextstep.api.dto.profile.DeleteProfileResponse;
import com.softcode.nextstep.api.dto.profile.ProfileResponse;
import com.softcode.nextstep.api.dto.profile.ProfileStatsDto;
import com.softcode.nextstep.api.dto.profile.ProfileUpdateRequest;
import com.softcode.nextstep.domain.journey.Journey;
import com.softcode.nextstep.domain.journey.JourneyStatus;
import com.softcode.nextstep.domain.resume.ResumeAnalysis;
import com.softcode.nextstep.domain.user.User;
import com.softcode.nextstep.repository.JourneyRepository;
import com.softcode.nextstep.repository.ResumeAnalysisRepository;
import com.softcode.nextstep.security.AuthenticatedUserContext;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AuthenticatedUserContext authenticatedUserContext;
    private final UserService userService;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final JourneyRepository journeyRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        User user = authenticatedUserContext.getCurrentUser();
        ProfileStatsDto stats = assembleStats(user);
        return new ProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCurrentJob(),
                null,
                user.getCreatedAt(),
                stats);
    }

    @Transactional
    public ProfileResponse updateProfile(ProfileUpdateRequest request) {
        User user = authenticatedUserContext.getCurrentUser();
        User updated = userService.updateProfile(user, request.name(), request.currentJob(), request.email());
        return new ProfileResponse(
                updated.getId(),
                updated.getName(),
                updated.getEmail(),
                updated.getCurrentJob(),
                null,
                updated.getCreatedAt(),
                assembleStats(updated));
    }

    @Transactional
    public DeleteProfileResponse deleteProfile() {
        User user = authenticatedUserContext.getCurrentUser();
        userService.deleteUser(user);
        return new DeleteProfileResponse("Conta excluida com sucesso", true);
    }

    private ProfileStatsDto assembleStats(User user) {
        long totalJourneys = userService.countJourneys(user);
        long completedJourneys = userService.countCompletedJourneys(user);
        Optional<ResumeAnalysis> analysis = resumeAnalysisRepository.findTopByUserOrderByAnalyzedAtDesc(user);
        int totalSkills = analysis
                .map(value -> readSkills(value.getSkillsJson()))
                .orElse(0);
        int averageProgress = journeyRepository
                .findTopByUserAndStatusOrderByCreatedAtDesc(user, JourneyStatus.ACTIVE)
                .map(Journey::getOverallProgress)
                .orElse(0);
        return new ProfileStatsDto((int) totalJourneys, (int) completedJourneys, totalSkills, averageProgress);
    }

    private int readSkills(String json) {
        if (json == null) {
            return 0;
        }
        try {
            List<SkillDto> skills = objectMapper.readValue(json, new TypeReference<List<SkillDto>>() {});
            return skills.size();
        } catch (Exception ex) {
            return 0;
        }
    }
}
