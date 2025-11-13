package com.softcode.nextstep.repository;

import com.softcode.nextstep.domain.journey.Journey;
import com.softcode.nextstep.domain.journey.JourneyStatus;
import com.softcode.nextstep.domain.user.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JourneyRepository extends JpaRepository<Journey, UUID> {

    Optional<Journey> findTopByUserAndStatusOrderByCreatedAtDesc(User user, JourneyStatus status);

    List<Journey> findByUserAndStatus(User user, JourneyStatus status);

    List<Journey> findByUser(User user);

    boolean existsByUserAndStatus(User user, JourneyStatus status);

    long countByUser(User user);

    long countByUserAndStatus(User user, JourneyStatus status);
}
