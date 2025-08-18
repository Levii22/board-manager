// src/main/java/com/board/manager/service/NotificationConsumer.java
package com.board.manager.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationConsumer {

    @RabbitListener(queues = "${app.rabbitmq.notification-queue}")
    public void handleNotification(NotificationService.NotificationPayload payload) {
        log.info("Received notification for user {}: {}", payload.getUserId(), payload.getMessage());
        // Add more logic if needed, e.g., saving to a database, sending an email, etc.
    }
}