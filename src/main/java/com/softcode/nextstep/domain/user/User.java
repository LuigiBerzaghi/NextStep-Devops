package com.softcode.nextstep.domain.user;

import com.softcode.nextstep.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "firebase_uid", nullable = false, unique = true, length = 128)
    private String firebaseUid;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 150)
    private String name;

    @Column(name = "current_job", length = 150)
    private String currentJob;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status = UserStatus.ACTIVE;
}

