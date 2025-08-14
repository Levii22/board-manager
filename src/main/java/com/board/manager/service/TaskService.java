package com.board.manager.service;

import com.board.manager.dto.TaskDto;
import com.board.manager.request.CreateTaskRequest;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

public interface TaskService {
    TaskDto createTask(Integer boardId, CreateTaskRequest request, Authentication authentication);
    List<TaskDto> getTasksByBoardId(Integer boardId);
    void deleteTask(Integer boardId, UUID taskId);
}