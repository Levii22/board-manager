package com.board.manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionService {

    private static final String SESSION_PREFIX = "ws:session:";
    private static final String BOARD_USERS_PREFIX = "ws:board:";
    private static final String USER_BOARDS_PREFIX = "ws:user:boards:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${websocket.session.timeout.hours:24}")
    private int sessionTimeoutHours;

    public void registerSession(String sessionId, String username) {
        Assert.hasText(sessionId, "Session ID must not be empty");
        Assert.hasText(username, "Username must not be empty");

        try {
            String key = SESSION_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, username, sessionTimeoutHours, TimeUnit.HOURS);
            if (log.isDebugEnabled()) {
                log.debug("Registered WebSocket session {} for user {}", sessionId, username);
            }
        } catch (Exception e) {
            log.error("Failed to register session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Failed to register session", e);
        }
    }

    public void unregisterSession(String sessionId) {
        Assert.hasText(sessionId, "Session ID must not be empty");

        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            redisTemplate.delete(sessionKey);
            if (log.isDebugEnabled()) {
                log.debug("Unregistered WebSocket session {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Failed to unregister session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Failed to unregister session", e);
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

    public void addUserToBoard(Integer boardId, String username) {
        Assert.notNull(boardId, "Board ID must not be null");
        Assert.hasText(username, "Username must not be empty");

        try {
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                connection.multi();
                String boardKey = BOARD_USERS_PREFIX + boardId;
                String userBoardsKey = USER_BOARDS_PREFIX + username;
                redisTemplate.opsForSet().add(boardKey, username);
                redisTemplate.opsForSet().add(userBoardsKey, boardId.toString());
                redisTemplate.expire(boardKey, sessionTimeoutHours, TimeUnit.HOURS);
                redisTemplate.expire(userBoardsKey, sessionTimeoutHours, TimeUnit.HOURS);
                connection.exec();
                return null;
            });
            if (log.isDebugEnabled()) {
                log.debug("Added user {} to board {}", username, boardId);
            }
        } catch (Exception e) {
            log.error("Failed to add user {} to board {}: {}", username, boardId, e.getMessage());
            throw new RuntimeException("Failed to add user to board", e);
        }
    }

    public void removeUserFromBoard(Integer boardId, String username) {
        Assert.notNull(boardId, "Board ID must not be null");
        Assert.hasText(username, "Username must not be empty");

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
            throw new RuntimeException("Failed to remove user from board", e);
        }
    }

    /**
     * Get all active users for a board
     */
    public Set<String> getBoardUsers(Integer boardId) {
        Assert.notNull(boardId, "Board ID must not be null");

        try {
            String key = BOARD_USERS_PREFIX + boardId;
            Set<String> users = redisTemplate.opsForSet().members(key);
            return users != null ? users : Set.of();
        } catch (Exception e) {
            log.error("Failed to get board users for {}: {}", boardId, e.getMessage());
            return Set.of();
        }
    }

    public boolean isUserActiveOnBoard(Integer boardId, String username) {
        Assert.notNull(boardId, "Board ID must not be null");
        Assert.hasText(username, "Username must not be empty");

        try {
            String key = BOARD_USERS_PREFIX + boardId;
            Boolean isMember = redisTemplate.opsForSet().isMember(key, username);
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.error("Failed to check user {} active status on board {}: {}", username, boardId, e.getMessage());
            return false;
        }
    }

    public Set<Integer> getUserBoards(String username) {
        Assert.hasText(username, "Username must not be empty");

        try {
            String key = USER_BOARDS_PREFIX + username;
            Set<String> boardStrings = redisTemplate.opsForSet().members(key);
            return boardStrings != null ?
                    boardStrings.stream()
                            .filter(Objects::nonNull)
                            .map(Integer::parseInt)
                            .collect(Collectors.toSet()) : Set.of();
        } catch (Exception e) {
            log.error("Failed to get user boards for {}: {}", username, e.getMessage());
            return Set.of();
        }
    }
}