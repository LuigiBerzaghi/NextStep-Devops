package com.softcode.nextstep.messaging;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${messaging.notifications.exchange}")
    private String exchange;

    @Value("${messaging.notifications.routing-key}")
    private String routingKey;

    @Value("${messaging.notifications.enabled:true}")
    private boolean notificationsEnabled;

    public void sendNotification(NotificationMessage message) {
        if (!notificationsEnabled) {
            return;
        }
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    public void notifyJourneyGenerated(UUID userId, UUID journeyId, String desiredJob) {
        sendNotification(new NotificationMessage(
                "journey.generated",
                userId,
                Map.of("journeyId", journeyId, "desiredJob", desiredJob == null ? "" : desiredJob)));
    }

    public void notifyResumeAnalyzed(UUID userId, UUID analysisId) {
        sendNotification(new NotificationMessage(
                "resume.analysis.completed", userId, Map.of("analysisId", analysisId.toString())));
    }
}
