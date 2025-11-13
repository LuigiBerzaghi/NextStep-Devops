package com.softcode.nextstep.repository;

import com.softcode.nextstep.domain.chat.ChatMessage;
import com.softcode.nextstep.domain.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findTop50ByUserAndConversationIdOrderByTimestampDesc(User user, String conversationId);

    List<ChatMessage> findByUserAndConversationIdOrderByTimestampAsc(User user, String conversationId);

    void deleteByUser(User user);
}
