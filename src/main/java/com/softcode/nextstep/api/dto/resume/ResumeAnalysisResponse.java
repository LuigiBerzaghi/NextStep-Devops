package com.softcode.nextstep.api.dto.resume;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResumeAnalysisResponse(UUID analysisId, ResumeSummaryDto summary, LocalDateTime analyzedAt) {
}

