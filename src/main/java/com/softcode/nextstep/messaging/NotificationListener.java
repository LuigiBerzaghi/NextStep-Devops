package com.softcode.nextstep.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "messaging.notifications.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class NotificationListener {

    @RabbitListener(queues = "${messaging.notifications.queue}")
    public void handleNotification(NotificationMessage message) {
        log.info("Processando evento async [{}] para usuario {} payload={}", message.type(), message.userId(), message.payload());
    }
}
