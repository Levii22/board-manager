package com.board.manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketConnectionManager {

    private static final String TOPIC_PREFIX = "/topic/board/";

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionService sessionService;

    @Async
    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Authentication auth = (Authentication) accessor.getUser();

        if (auth == null) {
            log.warn("Session connect event without authentication: {}", sessionId);
            return;
        }

        String username = auth.getName();
        Assert.hasText(username, "Username must not be empty");
        sessionService.registerSession(sessionId, username);
        log.info("WebSocket session connected: {} for user: {}", sessionId, username);
    }

    @Async
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (sessionId != null) {
            String username = sessionService.getSessionUser(sessionId);

            if (username != null) {
                // Get all boards this user was connected to - now using username instead of sessionId
                Set<Integer> userBoards = sessionService.getUserBoards(username);

                // Notify all boards that user left
                for (Integer boardId : userBoards) {
                    sessionService.removeUserFromBoard(boardId, username);
                    broadcastUserLeft(boardId, username);
                }

                sessionService.unregisterSession(sessionId);
                log.info("WebSocket session disconnected: {} for user: {} (cleaned up {} boards)",
                        sessionId, username, userBoards.size());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Session disconnect for untracked session: {}", sessionId);
            }
        }
    }

    @Async
    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        Authentication auth = (Authentication) accessor.getUser();

        if (destination != null && destination.startsWith(TOPIC_PREFIX) && auth != null) {
            try {
                Integer boardId = extractBoardIdFromDestination(destination);
                String username = auth.getName();
                if (log.isDebugEnabled()) {
                    log.debug("User {} subscribed to board {} topic via session {}",
                            username, boardId, sessionId);
                }
            } catch (IllegalArgumentException e) {
                log.warn("Error handling subscription to {}: {}", destination, e.getMessage());
            }
        }
    }

    @Async
    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();

        if (log.isDebugEnabled()) {
            log.debug("Session {} unsubscribed from subscription {} (destination: {})",
                    sessionId, subscriptionId, accessor.getDestination());
        }
    }

    public boolean addUserToBoard(String sessionId, Integer boardId, String username) {
        Assert.hasText(sessionId, "Session ID must not be empty");
        Assert.notNull(boardId, "Board ID must not be null");
        Assert.hasText(username, "Username must not be empty");

        if (sessionService.isUserActiveOnBoard(boardId, username)) {
            return false;
        }

        sessionService.addUserToBoard(boardId, username);
        return true;
    }

    public void removeUserFromBoard(Integer boardId, String username) {
        Assert.notNull(boardId, "Board ID must not be null");
        Assert.hasText(username, "Username must not be empty");
        sessionService.removeUserFromBoard(boardId, username);
    }

    public Set<String> getActiveBoardUsers(Integer boardId) {
        Assert.notNull(boardId, "Board ID must not be null");
        return sessionService.getBoardUsers(boardId);
    }

    private void broadcastUserLeft(Integer boardId, String username) {
        try {
            com.board.manager.dto.BoardUpdateDto update = com.board.manager.dto.BoardUpdateDto.builder()
                    .type(com.board.manager.dto.BoardUpdateDto.UpdateType.USER_LEFT)
                    .boardId(boardId)
                    .username(username)
                    .message(username + " disconnected from the board")
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend(TOPIC_PREFIX + boardId, update);
            if (log.isDebugEnabled()) {
                log.debug("Broadcasted user left for {} on board {}", username, boardId);
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast user left message for board {}: {}", boardId, e.getMessage());
        }
    }

    private Integer extractBoardIdFromDestination(String destination) {
        Assert.hasText(destination, "Destination must not be empty");
        String[] parts = destination.split("/");
        if (parts.length >= 4) {
            try {
                return Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                log.error("Invalid board ID in destination: {}", destination, e);
                throw new IllegalArgumentException("Invalid board ID in destination: " + destination, e);
            }
        }
        log.warn("Invalid destination format: {}", destination);
        throw new IllegalArgumentException("Invalid destination format: " + destination);
    }
}