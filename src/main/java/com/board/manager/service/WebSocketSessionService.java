package com.board.manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String SESSION_PREFIX = "ws:session:";
    private static final String BOARD_USERS_PREFIX = "ws:board:";
    private static final String USER_BOARDS_PREFIX = "ws:user:boards:"; // Track boards per user
    private static final int SESSION_TIMEOUT_HOURS = 24;

    /**
     * Register a WebSocket session
     */
    public void registerSession(String sessionId, String username) {
        try {
            String key = SESSION_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, username, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);
            log.debug("Registered WebSocket session {} for user {}", sessionId, username);
        } catch (Exception e) {
            log.error("Failed to register session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Unregister a WebSocket session - SIMPLIFIED VERSION
     */
    public void unregisterSession(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            redisTemplate.delete(sessionKey);
            log.debug("Unregistered WebSocket session {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to unregister session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Get username for a session
     */
    public String getSessionUser(String sessionId) {
        try {
            String key = SESSION_PREFIX + sessionId;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get session user {}: {}", sessionId, e.getMessage());
            return null;
        }
    }

    /**
     * Add user to a board's active users set
     */
    public void addUserToBoard(Integer boardId, String username) {
        try {
            String boardKey = BOARD_USERS_PREFIX + boardId;
            String userBoardsKey = USER_BOARDS_PREFIX + username;

            SetOperations<String, String> setOps = redisTemplate.opsForSet();

            // Add user to board
            setOps.add(boardKey, username);
            redisTemplate.expire(boardKey, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);

            // Add board to user's boards
            setOps.add(userBoardsKey, boardId.toString());
            redisTemplate.expire(userBoardsKey, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);

            log.debug("Added user {} to board {}", username, boardId);
        } catch (Exception e) {
            log.error("Failed to add user {} to board {}: {}", username, boardId, e.getMessage());
        }
    }

    /**
     * Remove user from a board's active users set
     */
    public void removeUserFromBoard(Integer boardId, String username) {
        try {
            String boardKey = BOARD_USERS_PREFIX + boardId;
            String userBoardsKey = USER_BOARDS_PREFIX + username;

            SetOperations<String, String> setOps = redisTemplate.opsForSet();

            // Remove user from board
            setOps.remove(boardKey, username);

            // Remove board from user's boards
            setOps.remove(userBoardsKey, boardId.toString());

            // Clean up empty sets
            Long boardSize = setOps.size(boardKey);
            if (boardSize != null && boardSize == 0) {
                redisTemplate.delete(boardKey);
            }

            Long userBoardsSize = setOps.size(userBoardsKey);
            if (userBoardsSize != null && userBoardsSize == 0) {
                redisTemplate.delete(userBoardsKey);
            }

            log.debug("Removed user {} from board {}", username, boardId);
        } catch (Exception e) {
            log.error("Failed to remove user {} from board {}: {}", username, boardId, e.getMessage());
        }
    }

    /**
     * Get all active users for a board
     */
    public Set<String> getBoardUsers(Integer boardId) {
        try {
            String key = BOARD_USERS_PREFIX + boardId;
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("Failed to get board users for {}: {}", boardId, e.getMessage());
            return Set.of();
        }
    }

    /**
     * Check if user is active on a board
     */
    public boolean isUserActiveOnBoard(Integer boardId, String username) {
        try {
            String key = BOARD_USERS_PREFIX + boardId;
            Boolean isMember = redisTemplate.opsForSet().isMember(key, username);
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.error("Failed to check user {} active status on board {}: {}", username, boardId, e.getMessage());
            return false;
        }
    }

    /**
     * Get all boards for a user
     */
    public Set<Integer> getUserBoards(String username) {
        try {
            String key = USER_BOARDS_PREFIX + username;
            Set<String> boardStrings = redisTemplate.opsForSet().members(key);
            if (boardStrings != null) {
                return boardStrings.stream()
                        .map(Integer::parseInt)
                        .collect(Collectors.toSet());
            }
            return Set.of();
        } catch (Exception e) {
            log.error("Failed to get user boards for {}: {}", username, e.getMessage());
            return Set.of();
        }
    }
}
