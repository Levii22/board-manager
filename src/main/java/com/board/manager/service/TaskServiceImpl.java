package com.board.manager.service;

import com.board.manager.dto.TaskDto;
import com.board.manager.mapper.TaskMapper;
import com.board.manager.model.Board;
import com.board.manager.model.Task;
import com.board.manager.repository.BoardRepository;
import com.board.manager.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

    @Override
    @Caching(evict = {
        @CacheEvict(value = "tasks", key = "'board:' + #boardId"),
        @CacheEvict(value = "board", key = "#boardId")
    })
    public TaskDto createTask(Integer boardId, TaskDto taskDto) {
        Task task = taskMapper.toEntity(taskDto);
        Board board = getBoardOrThrow(boardId);
        task.setBoard(board);
        Task saved = taskRepository.save(task);
        log.debug("Created task {} for board {} and invalidated cache", saved.getId(), boardId);
        return taskMapper.toDto(saved);
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
    public void deleteTask(Integer boardId, UUID taskId) {
        getBoardOrThrow(boardId);
        taskRepository.deleteByIdAndBoardId(taskId, boardId);

        // Invalidate related caches
        cacheService.evictTaskCache(boardId);
        log.debug("Deleted task {} from board {} and invalidated cache", taskId, boardId);
    }

    private Board getBoardOrThrow(Integer boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException("Board not found"));
    }
}