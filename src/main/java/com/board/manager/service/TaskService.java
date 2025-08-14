package com.board.manager.service;

import com.board.manager.dto.TaskDto;
import java.util.List;
import java.util.UUID;

public interface TaskService {
    TaskDto createTask(Integer boardId, TaskDto taskDto);
    List<TaskDto> getTasksByBoardId(Integer boardId);
    void deleteTask(Integer boardId, UUID taskId);
}