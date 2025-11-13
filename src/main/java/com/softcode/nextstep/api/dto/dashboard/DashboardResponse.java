package com.softcode.nextstep.api.dto.dashboard;

import com.softcode.nextstep.api.dto.common.SkillDto;
import com.softcode.nextstep.api.dto.common.SuggestedPathDto;
import java.util.List;

public record DashboardResponse(
        DashboardUserDto user,
        DashboardNextStepDto nextStep,
        List<SkillDto> skills,
        List<DashboardTrendDto> trends,
        List<SuggestedPathDto> suggestedPaths) {
}

