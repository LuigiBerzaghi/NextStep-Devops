package com.softcode.nextstep.domain.chat;

import com.softcode.nextstep.domain.BaseEntity;
import com.softcode.nextstep.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chat_messages")
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChatRole role;

    @Column(nullable = false, columnDefinition = "CLOB")
    private String message;

    @Column(nullable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime timestamp;
}
