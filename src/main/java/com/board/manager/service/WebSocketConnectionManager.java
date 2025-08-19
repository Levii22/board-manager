package com.board.manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketConnectionManager {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionService sessionService;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Authentication auth = (Authentication) accessor.getUser();

        if (auth != null && sessionId != null) {
            String username = auth.getName();
            sessionService.registerSession(sessionId, username);
            log.info("WebSocket session connected: {} for user: {}", sessionId, username);
        } else {
            log.warn("Session connect event without proper authentication: {}", sessionId);
        }
    }

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
            } else {
                log.debug("Session disconnect for untracked session: {}", sessionId);
            }
        }
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        Authentication auth = (Authentication) accessor.getUser();

        if (destination != null && destination.startsWith("/topic/board/") && auth != null) {
            try {
                Integer boardId = extractBoardIdFromDestination(destination);
                if (boardId != null) {
                    String username = auth.getName();
                    log.debug("User {} subscribed to board {} topic via session {}",
                            username, boardId, sessionId);
                }
            } catch (Exception e) {
                log.warn("Error handling subscription to {}: {}", destination, e.getMessage());
            }
        }
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();

        log.debug("Session {} unsubscribed from subscription {}", sessionId, subscriptionId);
    }

    /**
     * Track user joining a board
     */
    public boolean addUserToBoard(String sessionId, Integer boardId, String username) {
        // Check if user is already on this board
        if (sessionService.isUserActiveOnBoard(boardId, username)) {
            return false;
        }

        // Add user to board - no more session-board tracking needed
        sessionService.addUserToBoard(boardId, username);
        return true;
    }

    /**
     * Track user leaving a board
     */
    public void removeUserFromBoard(Integer boardId, String username) {
        sessionService.removeUserFromBoard(boardId, username);
    }

    /**
     * Get active users for a board
     */
    public Set<String> getActiveBoardUsers(Integer boardId) {
        return sessionService.getBoardUsers(boardId);
    }

    /**
     * Broadcast user left message
     */
    private void broadcastUserLeft(Integer boardId, String username) {
        try {
            com.board.manager.dto.BoardUpdateDto update = com.board.manager.dto.BoardUpdateDto.builder()
                    .type(com.board.manager.dto.BoardUpdateDto.UpdateType.USER_LEFT)
                    .boardId(boardId)
                    .username(username)
                    .message(username + " disconnected from the board")
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/board/" + boardId, update);
            log.debug("Broadcasted user left for {} on board {}", username, boardId);
        } catch (Exception e) {
            log.warn("Failed to broadcast user left message: {}", e.getMessage());
        }
    }

    /**
     * Extract board ID from WebSocket destination
     */
    private Integer extractBoardIdFromDestination(String destination) {
        String[] parts = destination.split("/");
        if (parts.length >= 4) {
            try {
                return Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                log.warn("Invalid board ID in destination: {}", destination);
            }
        }
        return null;
    }
}
