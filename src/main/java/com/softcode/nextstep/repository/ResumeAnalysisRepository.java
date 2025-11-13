package com.softcode.nextstep.repository;

import com.softcode.nextstep.domain.resume.ResumeAnalysis;
import com.softcode.nextstep.domain.user.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, UUID> {

    Optional<ResumeAnalysis> findTopByUserOrderByAnalyzedAtDesc(User user);

    void deleteByUser(User user);
}
