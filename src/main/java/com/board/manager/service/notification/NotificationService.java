package com.board.manager.service.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.io.Serializable;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.notification-queue}")
    private String notificationQueue;

    public void sendTaskAssignmentNotification(Integer userId, String message) {
        NotificationPayload payload = new NotificationPayload(userId, message);
        rabbitTemplate.convertAndSend(notificationQueue, payload);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationPayload implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer userId;
        private String message;
    }
}