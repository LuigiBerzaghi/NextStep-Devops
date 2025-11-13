package com.softcode.nextstep.service;

import com.softcode.nextstep.domain.journey.Journey;
import com.softcode.nextstep.domain.journey.JourneyStatus;
import com.softcode.nextstep.domain.user.User;
import com.softcode.nextstep.domain.user.UserStatus;
import com.softcode.nextstep.exception.BadRequestException;
import com.softcode.nextstep.exception.NotFoundException;
import com.softcode.nextstep.repository.ChatMessageRepository;
import com.softcode.nextstep.repository.JourneyRepository;
import com.softcode.nextstep.repository.ResumeAnalysisRepository;
import com.softcode.nextstep.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final JourneyRepository journeyRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public User findOrCreate(String firebaseUid, String email) {
        return userRepository
                .findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    User user = new User();
                    user.setFirebaseUid(firebaseUid);
                    user.setEmail(email.toLowerCase(Locale.ROOT));
                    return userRepository.save(user);
                });
    }

    public User findByFirebaseUidOrThrow(String firebaseUid) {
        return userRepository
                .findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado"));
    }

    @Transactional
    public User updateProfile(User user, String name, String currentJob, String email) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(currentJob) || !StringUtils.hasText(email)) {
            throw new BadRequestException("Nome, email e cargo atual sao obrigatorios");
        }
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Email ja esta em uso");
        }
        user.setName(name);
        user.setCurrentJob(currentJob);
        user.setEmail(email.toLowerCase(Locale.ROOT));
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(User user) {
        resumeAnalysisRepository.deleteByUser(user);
        List<Journey> journeys = journeyRepository.findByUser(user);
        journeyRepository.deleteAll(journeys);
        chatMessageRepository.deleteByUser(user);
        user.setStatus(UserStatus.DELETED);
        userRepository.delete(user);
    }

    public boolean hasActiveJourney(User user) {
        return journeyRepository.existsByUserAndStatus(user, JourneyStatus.ACTIVE);
    }

    public long countJourneys(User user) {
        return journeyRepository.countByUser(user);
    }

    public long countCompletedJourneys(User user) {
        return journeyRepository.countByUserAndStatus(user, JourneyStatus.COMPLETED);
    }
}
