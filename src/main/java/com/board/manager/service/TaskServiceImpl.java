package com.board.manager.service;

import com.board.manager.dto.TaskDto;
import com.board.manager.mapper.TaskMapper;
import com.board.manager.model.Board;
import com.board.manager.model.Task;
import com.board.manager.model.User;
import com.board.manager.repository.BoardRepository;
import com.board.manager.repository.TaskRepository;
import com.board.manager.repository.UserRepository;
import com.board.manager.request.CreateTaskRequest;
import com.board.manager.request.UpdateTaskRequest;
import com.board.manager.service.notification.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final BoardRepository boardRepository;
    private final CacheService cacheService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final WebSocketService webSocketService;

    @Override
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "'board:' + #boardId"),
            @CacheEvict(value = "board", key = "#boardId")
    })
    public TaskDto createTask(Integer boardId, CreateTaskRequest request, @AuthenticationPrincipal User currentUser) {
        Task task = taskMapper.toEntity(request);
        Board board = getBoardOrThrow(boardId);
        task.setBoard(board);
        task.setOwner(currentUser);

        if (request.getAssignedTo() != null) {
            User user = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            task.setAssignedTo(user);
        }
        Task saved = taskRepository.save(task);
        TaskDto taskDto = taskMapper.toDto(saved);

        // Send notifications
        if (saved.getAssignedTo() != null) {
            notificationService.sendTaskAssignmentNotification(
                    saved.getAssignedTo().getId(),
                    "You have been assigned a new task: " + saved.getTitle()
            );
            // Broadcast task assignment
            webSocketService.broadcastTaskAssigned(
                    boardId,
                    taskDto,
                    currentUser.getUsername(),
                    saved.getAssignedTo().getUsername(),
                    saved.getAssignedTo().getId()
            );
        } else {
            // Broadcast task creation
            webSocketService.broadcastTaskCreated(boardId, taskDto, currentUser.getUsername());
        }

        log.debug("Created task {} for board {} and invalidated cache", saved.getId(), boardId);
        return taskDto;
    }

    @Override
    @Cacheable(value = "tasks", key = "'board:' + #boardId")
    public List<TaskDto> getTasksByBoardId(Integer boardId) {
        log.debug("Getting tasks for board {} (cache miss)", boardId);
        getBoardOrThrow(boardId);
        return taskRepository.findByBoardId(boardId)
                .stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTask(Integer boardId, UUID taskId, User currentUser) {
        getBoardOrThrow(boardId);

        // Get task details before deletion for broadcasting
        Task task = taskRepository.findByIdAndBoardId(taskId, boardId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        String taskTitle = task.getTitle();

        taskRepository.deleteByIdAndBoardId(taskId, boardId);

        // Broadcast task deletion
        webSocketService.broadcastTaskDeleted(boardId, taskId, currentUser.getUsername(), taskTitle);

        // Invalidate related caches
        cacheService.evictTaskCache(boardId);
        log.debug("Deleted task {} from board {} and invalidated cache", taskId, boardId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "'board:' + #boardId"),
            @CacheEvict(value = "board", key = "#boardId")
    })
    public TaskDto updateTask(Integer boardId, UUID taskId, UpdateTaskRequest request, User currentUser) {
        getBoardOrThrow(boardId);

        Task task = taskRepository.findByIdAndBoardId(taskId, boardId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        boolean hasChanges = false;
        User previousAssignee = task.getAssignedTo();

        // Update only provided fields
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            task.setTitle(request.getTitle());
            hasChanges = true;
        }

        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            task.setDescription(request.getDescription());
            hasChanges = true;
        }

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            task.setStatus(Task.Status.valueOf(request.getStatus().toUpperCase()));
            hasChanges = true;
        }

        if (request.getAssignedTo() != null) {
            if (request.getAssignedTo() == 0) {
                // Unassign task
                task.setAssignedTo(null);
                hasChanges = true;
            } else {
                User assignee = userRepository.findById(request.getAssignedTo())
                        .orElseThrow(() -> new EntityNotFoundException("User not found"));
                task.setAssignedTo(assignee);
                hasChanges = true;

                // Send notification if assignment changed
                if (previousAssignee == null || !previousAssignee.getId().equals(assignee.getId())) {
                    notificationService.sendTaskAssignmentNotification(
                            assignee.getId(),
                            "You have been assigned task: " + task.getTitle()
                    );
                }
            }
        }

        if (!hasChanges) {
            log.debug("No changes detected for task {} on board {}", taskId, boardId);
            return taskMapper.toDto(task);
        }

        Task saved = taskRepository.save(task);
        TaskDto taskDto = taskMapper.toDto(saved);

        // Broadcast appropriate update type
        if (request.getAssignedTo() != null &&
                (previousAssignee == null ||
                        (saved.getAssignedTo() != null && !previousAssignee.getId().equals(saved.getAssignedTo().getId())))) {
            // Assignment changed
            if (saved.getAssignedTo() != null) {
                webSocketService.broadcastTaskAssigned(
                        boardId,
                        taskDto,
                        currentUser.getUsername(),
                        saved.getAssignedTo().getUsername(),
                        saved.getAssignedTo().getId()
                );
            } else {
                webSocketService.broadcastTaskUpdated(boardId, taskDto, currentUser.getUsername());
            }
        } else {
            // Regular update
            webSocketService.broadcastTaskUpdated(boardId, taskDto, currentUser.getUsername());
        }

        log.debug("Updated task {} on board {}", taskId, boardId);
        return taskDto;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "'board:' + #boardId"),
            @CacheEvict(value = "board", key = "#boardId")
    })
    public TaskDto assignTask(Integer boardId, UUID taskId, Integer assigneeId, User currentUser) {
        getBoardOrThrow(boardId);

        Task task = taskRepository.findByIdAndBoardId(taskId, boardId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        task.setAssignedTo(assignee);
        Task saved = taskRepository.save(task);
        TaskDto taskDto = taskMapper.toDto(saved);

        // Send notification and broadcast
        notificationService.sendTaskAssignmentNotification(
                assigneeId,
                "You have been assigned task: " + task.getTitle()
        );

        webSocketService.broadcastTaskAssigned(
                boardId,
                taskDto,
                currentUser.getUsername(),
                assignee.getUsername(),
                assigneeId
        );

        log.debug("Assigned task {} to user {} on board {}", taskId, assigneeId, boardId);
        return taskDto;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "'board:' + #boardId"),
            @CacheEvict(value = "board", key = "#boardId")
    })
    public TaskDto updateTaskStatus(Integer boardId, UUID taskId, String status, User currentUser) {
        getBoardOrThrow(boardId);

        Task task = taskRepository.findByIdAndBoardId(taskId, boardId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        task.setStatus(Task.Status.valueOf(status.toUpperCase()));
        Task saved = taskRepository.save(task);
        TaskDto taskDto = taskMapper.toDto(saved);

        // Broadcast status update
        webSocketService.broadcastTaskUpdated(boardId, taskDto, currentUser.getUsername());

        log.debug("Updated task {} status to {} on board {}", taskId, status, boardId);
        return taskDto;
    }

    private Board getBoardOrThrow(Integer boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException("Board not found"));
    }
}