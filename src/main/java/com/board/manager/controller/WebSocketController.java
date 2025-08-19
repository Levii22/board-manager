package com.board.manager.controller;

import com.board.manager.dto.BoardUpdateDto;
import com.board.manager.model.User;
import com.board.manager.service.BoardService;
import com.board.manager.service.WebSocketConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final BoardService boardAccessService;
    private final WebSocketConnectionManager connectionManager;

    @MessageMapping("/board/{boardId}/join")
    public void joinBoard(@DestinationVariable Integer boardId, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        try {
            if (principal == null) {
                log.warn("Unauthenticated user attempted to join board {}", boardId);
                sendErrorToUser(null, "Authentication required");
                return;
            }

            User currentUser = getUserFromPrincipal(principal);
            String username = currentUser.getUsername();
            String sessionId = headerAccessor.getSessionId();

            log.info("User {} attempting to join board {} via session {}", username, boardId, sessionId);

            // Validate board access permissions using RBAC
            if (!boardAccessService.canUserAccessBoard(boardId, currentUser)) {
                log.warn("User {} denied access to board {}", username, boardId);
                sendErrorToUser(username, "You do not have access to this board");
                return;
            }

            // Check if user is already in the board (prevent duplicate joins)
            boolean isNewJoin = connectionManager.addUserToBoard(sessionId, boardId, username);

            if (!isNewJoin) {
                log.debug("User {} already joined board {}", username, boardId);
                sendConfirmationToUser(username, "Already connected to board " + boardId);
                return;
            }

            // Broadcast user joined to all board subscribers
            BoardUpdateDto update = BoardUpdateDto.builder()
                    .type(BoardUpdateDto.UpdateType.USER_JOINED)
                    .boardId(boardId)
                    .userId(currentUser.getId())
                    .username(username)
                    .message(username + " joined the board")
                    .timestamp(LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/board/" + boardId, update);

            // Send active users list to the joining user
            var activeUsers = connectionManager.getActiveBoardUsers(boardId);
            sendConfirmationToUser(username, "Connected to board " + boardId +
                    ". Active users: " + activeUsers.size());

            log.info("User {} successfully joined board {} (total active: {})",
                    username, boardId, activeUsers.size());

        } catch (Exception e) {
            log.error("Error in joinBoard for board {}: {}", boardId, e.getMessage(), e);
            sendErrorToUser(principal != null ? principal.getName() : "unknown",
                    "Failed to join board: " + e.getMessage());
        }
    }

    @MessageMapping("/board/{boardId}/leave")
    public void leaveBoard(@DestinationVariable Integer boardId, Principal principal) {
        try {
            if (principal == null) {
                log.warn("Unauthenticated user attempted to leave board {}", boardId);
                return;
            }

            User currentUser = getUserFromPrincipal(principal);
            String username = currentUser.getUsername();

            // Check if user is actually connected to this board
            var activeUsers = connectionManager.getActiveBoardUsers(boardId);
            if (!activeUsers.contains(username)) {
                log.debug("User {} is not active on board {}, ignoring leave", username, boardId);
                return;
            }

            log.info("User {} leaving board {}", username, boardId);

            // Remove user from board tracking
            connectionManager.removeUserFromBoard(boardId, username);

            // Broadcast user left message
            BoardUpdateDto update = BoardUpdateDto.builder()
                    .type(BoardUpdateDto.UpdateType.USER_LEFT)
                    .boardId(boardId)
                    .userId(currentUser.getId())
                    .username(username)
                    .message(username + " left the board")
                    .timestamp(LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/board/" + boardId, update);
            log.info("User {} successfully left board {}", username, boardId);

        } catch (Exception e) {
            log.error("Error in leaveBoard for board {}: {}", boardId, e.getMessage(), e);
            sendErrorToUser(principal != null ? principal.getName() : "unknown",
                    "Failed to leave board: " + e.getMessage());
        }
    }

    @MessageMapping("/board/{boardId}/ping")
    public void pingBoard(@DestinationVariable Integer boardId, Principal principal) {
        try {
            if (principal == null) {
                return;
            }

            User currentUser = getUserFromPrincipal(principal);
            String username = currentUser.getUsername();

            // Validate access and that user is active on board
            if (!boardAccessService.canUserAccessBoard(boardId, currentUser)) {
                sendErrorToUser(username, "Access denied to board " + boardId);
                return;
            }

            var activeUsers = connectionManager.getActiveBoardUsers(boardId);
            if (!activeUsers.contains(username)) {
                sendErrorToUser(username, "Not connected to board " + boardId);
                return;
            }

            // Send pong response
            sendConfirmationToUser(username, "pong from board " + boardId);
            log.debug("Ping/pong for user {} on board {}", username, boardId);

        } catch (Exception e) {
            log.error("Error in pingBoard for board {}: {}", boardId, e.getMessage());
            sendErrorToUser(principal.getName(),
                    "Ping failed: " + e.getMessage());
        }
    }

    /**
     * Exception handler for WebSocket message processing errors
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Exception ex, Principal principal) {
        String username = principal != null ? principal.getName() : "unknown";
        String errorMessage = "An error occurred: " + ex.getMessage();
        log.error("WebSocket error for user {}: {}", username, ex.getMessage(), ex);
        return errorMessage;
    }

    /**
     * Get User from Principal with enhanced error handling
     */
    private User getUserFromPrincipal(Principal principal) {
        if (principal instanceof Authentication authentication) {
            Object userPrincipal = authentication.getPrincipal();
            if (userPrincipal instanceof User user) {
                return user;
            } else {
                log.error("Invalid user principal type: {}", userPrincipal.getClass().getSimpleName());
                throw new AccessDeniedException("Invalid user authentication");
            }
        }
        log.error("Principal is not an Authentication instance: {}", principal.getClass().getSimpleName());
        throw new AccessDeniedException("Invalid authentication type");
    }

    /**
     * Send error message to specific user with fallback handling
     */
    private void sendErrorToUser(String username, String message) {
        try {
            if (username != null) {
                messagingTemplate.convertAndSendToUser(username, "/queue/errors", message);
                log.debug("Sent error to user {}: {}", username, message);
            } else {
                log.warn("Cannot send error message - username is null: {}", message);
            }
        } catch (Exception e) {
            log.error("Failed to send error message to user {}: {}", username, e.getMessage());
        }
    }

    /**
     * Send confirmation message to specific user
     */
    private void sendConfirmationToUser(String username, String message) {
        try {
            messagingTemplate.convertAndSendToUser(username, "/queue/confirmations", message);
            log.debug("Sent confirmation to user {}: {}", username, message);
        } catch (Exception e) {
            log.error("Failed to send confirmation to user {}: {}", username, e.getMessage());
        }
    }
}
