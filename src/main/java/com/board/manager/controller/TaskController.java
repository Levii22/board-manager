package com.board.manager.controller;

import com.board.manager.dto.TaskDto;
import com.board.manager.request.CreateTaskRequest;
import com.board.manager.service.TaskServiceImpl;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/board/{boardId}/task")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskServiceImpl taskService;


    @PostMapping
    public ResponseEntity<TaskDto> createTask(@PathVariable Integer boardId, @Valid @RequestBody CreateTaskRequest request, Authentication authentication) {
        return ResponseEntity.ok(taskService.createTask(boardId, request, authentication));
    }

    @GetMapping
    public ResponseEntity<List<TaskDto>> getTasks(@PathVariable Integer boardId) {
        return ResponseEntity.ok(taskService.getTasksByBoardId(boardId));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Integer boardId, @PathVariable UUID taskId) {
        taskService.deleteTask(boardId, taskId);
        return ResponseEntity.noContent().build();
    }
}