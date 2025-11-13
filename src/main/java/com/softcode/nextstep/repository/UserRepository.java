package com.softcode.nextstep.repository;

import com.softcode.nextstep.domain.user.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByFirebaseUid(String firebaseUid);

    boolean existsByEmail(String email);
}

