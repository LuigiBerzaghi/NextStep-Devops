package com.softcode.nextstep.repository;

import com.softcode.nextstep.domain.journey.JourneyStep;
import com.softcode.nextstep.domain.user.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JourneyStepRepository extends JpaRepository<JourneyStep, UUID> {

    @Query("select js from JourneyStep js where js.id = :id and js.journey.user = :user")
    Optional<JourneyStep> findByIdAndUser(@Param("id") UUID id, @Param("user") User user);
}

