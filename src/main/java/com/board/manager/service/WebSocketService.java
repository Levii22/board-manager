package com.board.manager.service;

import com.board.manager.dto.BoardUpdateDto;
import com.board.manager.dto.TaskDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast task creation to all board subscribers
     */
    public void broadcastTaskCreated(Integer boardId, TaskDto task, String creatorUsername) {
        BoardUpdateDto update = BoardUpdateDto.builder()
                .type(BoardUpdateDto.UpdateType.TASK_CREATED)
                .boardId(boardId)
                .taskId(task.getId())
                .taskData(task)
                .message(creatorUsername + " created a new task: " + task.getTitle())
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/board/" + boardId, update);
        log.debug("Broadcasted task creation for task {} on board {}", task.getId(), boardId);
    }

    /**
     * Broadcast task update to all board subscribers
     */
    public void broadcastTaskUpdated(Integer boardId, TaskDto task, String updaterUsername) {
        BoardUpdateDto update = BoardUpdateDto.builder()
                .type(BoardUpdateDto.UpdateType.TASK_UPDATED)
                .boardId(boardId)
                .taskId(task.getId())
                .taskData(task)
                .message(updaterUsername + " updated task: " + task.getTitle())
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/board/" + boardId, update);
        log.debug("Broadcasted task update for task {} on board {}", task.getId(), boardId);
    }

    /**
     * Broadcast task deletion to all board subscribers
     */
    public void broadcastTaskDeleted(Integer boardId, UUID taskId, String deleterUsername, String taskTitle) {
        BoardUpdateDto update = BoardUpdateDto.builder()
                .type(BoardUpdateDto.UpdateType.TASK_DELETED)
                .boardId(boardId)
                .taskId(taskId)
                .message(deleterUsername + " deleted task: " + taskTitle)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/board/" + boardId, update);
        log.debug("Broadcasted task deletion for task {} on board {}", taskId, boardId);
    }

    /**
     * Broadcast task assignment to all board subscribers and send private notification
     */
    public void broadcastTaskAssigned(Integer boardId, TaskDto task, String assignerUsername, String assigneeUsername, Integer assigneeId) {
        BoardUpdateDto update = BoardUpdateDto.builder()
                .type(BoardUpdateDto.UpdateType.TASK_ASSIGNED)
                .boardId(boardId)
                .taskId(task.getId())
                .taskData(task)
                .message(assignerUsername + " assigned task '" + task.getTitle() + "' to " + assigneeUsername)
                .timestamp(LocalDateTime.now())
                .build();

        // Broadcast to all board subscribers
        messagingTemplate.convertAndSend("/topic/board/" + boardId, update);

        // Send private notification to the assignee
        sendPrivateNotification(assigneeId, "You have been assigned a new task: " + task.getTitle());

        log.debug("Broadcasted task assignment for task {} on board {}", task.getId(), boardId);
    }

    /**
     * Send private notification to a specific user
     */
    public void sendPrivateNotification(Integer userId, String message) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            message
        );
        log.debug("Sent private notification to user {}: {}", userId, message);
    }

    /**
     * Broadcast board update (when board details change)
     */
    public void broadcastBoardUpdated(Integer boardId, String updaterUsername) {
        BoardUpdateDto update = BoardUpdateDto.builder()
                .type(BoardUpdateDto.UpdateType.BOARD_UPDATED)
                .boardId(boardId)
                .message(updaterUsername + " updated the board")
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/board/" + boardId, update);
        log.debug("Broadcasted board update for board {}", boardId);
    }
}
