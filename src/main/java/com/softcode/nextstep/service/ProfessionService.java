package com.softcode.nextstep.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softcode.nextstep.api.dto.common.CareerSuggestionDto;
import com.softcode.nextstep.api.dto.profession.ProfessionSuggestionItem;
import com.softcode.nextstep.api.dto.profession.ProfessionSuggestionResponse;
import com.softcode.nextstep.domain.resume.ResumeAnalysis;
import com.softcode.nextstep.domain.user.User;
import com.softcode.nextstep.exception.BadRequestException;
import com.softcode.nextstep.repository.ResumeAnalysisRepository;
import com.softcode.nextstep.security.AuthenticatedUserContext;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProfessionService {

    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final AuthenticatedUserContext authenticatedUserContext;
    private final ObjectMapper objectMapper;

    public ProfessionSuggestionResponse findSuggestions(String search) {
        User user = authenticatedUserContext.getCurrentUser();
        ResumeAnalysis analysis = resumeAnalysisRepository
                .findTopByUserOrderByAnalyzedAtDesc(user)
                .orElseThrow(() -> new BadRequestException("error.profession.resume_required"));
        List<CareerSuggestionDto> careerSuggestions = readSuggestions(analysis.getSuggestedCareersJson()).stream()
                .filter(Objects::nonNull)
                .toList();
        String category = resolveCategory(analysis);
        List<ProfessionSuggestionItem> items = IntStream.range(0, careerSuggestions.size())
                .mapToObj(index -> toItem(careerSuggestions.get(index), category, index))
                .toList();
        List<ProfessionSuggestionItem> filtered = filterBySearch(items, search);
        return new ProfessionSuggestionResponse(filtered);
    }

    private ProfessionSuggestionItem toItem(CareerSuggestionDto suggestion, String category, int index) {
        String title = StringUtils.hasText(suggestion.title()) ? suggestion.title() : "Profissao sugerida";
        String match = suggestion.match() == null ? "" : suggestion.match();
        String description = suggestion.reason() == null ? "" : suggestion.reason();
        return new ProfessionSuggestionItem(buildId(title, index), title, category, match, description);
    }

    private List<ProfessionSuggestionItem> filterBySearch(List<ProfessionSuggestionItem> items, String search) {
        if (!StringUtils.hasText(search)) {
            return items;
        }
        String normalized = search.toLowerCase(Locale.ROOT);
        return items.stream()
                .filter(item -> contains(item.title(), normalized)
                        || contains(item.category(), normalized)
                        || contains(item.description(), normalized))
                .toList();
    }

    private boolean contains(String value, String search) {
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private List<CareerSuggestionDto> readSuggestions(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<List<CareerSuggestionDto>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao ler sugestoes do curriculo", ex);
        }
    }

    private String buildId(String title, int index) {
        String normalized = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-|-$)", "");
        if (!StringUtils.hasText(normalized)) {
            normalized = "suggestion";
        }
        return normalized + "-" + (index + 1);
    }

    private String resolveCategory(ResumeAnalysis analysis) {
        if (StringUtils.hasText(analysis.getCurrentJob())) {
            return analysis.getCurrentJob();
        }
        if (StringUtils.hasText(analysis.getExperienceLevel())) {
            return analysis.getExperienceLevel();
        }
        return "Sugestao IA";
    }
}
