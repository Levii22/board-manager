package com.board.manager.service;

import com.board.manager.dto.TaskDto;
import com.board.manager.model.User;
import com.board.manager.request.CreateTaskRequest;
import com.board.manager.request.UpdateTaskRequest;

import java.util.List;
import java.util.UUID;

public interface TaskService {
    TaskDto createTask(Integer boardId, CreateTaskRequest request, User currentUser);
    List<TaskDto> getTasksByBoardId(Integer boardId);
    void deleteTask(Integer boardId, UUID taskId, User currentUser);
    TaskDto updateTask(Integer boardId, UUID taskId, UpdateTaskRequest request, User currentUser);
    TaskDto assignTask(Integer boardId, UUID taskId, Integer assigneeId, User currentUser);
    TaskDto updateTaskStatus(Integer boardId, UUID taskId, String status, User currentUser);
}