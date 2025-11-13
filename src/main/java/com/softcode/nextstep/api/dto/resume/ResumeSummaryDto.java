package com.softcode.nextstep.api.dto.resume;

import com.softcode.nextstep.api.dto.common.CareerSuggestionDto;
import com.softcode.nextstep.api.dto.common.SkillDto;
import java.util.List;

public record ResumeSummaryDto(
        List<SkillDto> currentSkills,
        String experienceLevel,
        String currentJob,
        int yearsOfExperience,
        List<String> gaps,
        List<CareerSuggestionDto> suggestedCareers) {
}

