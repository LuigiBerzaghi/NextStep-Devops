package com.softcode.nextstep.repository;

import com.softcode.nextstep.domain.chat.ChatMessage;
import com.softcode.nextstep.domain.user.User;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByUserAndConversationId(User user, String conversationId, Pageable pageable);

    void deleteByUser(User user);
}
