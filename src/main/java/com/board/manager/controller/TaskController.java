package com.board.manager.controller;

import com.board.manager.dto.TaskDto;
import com.board.manager.model.User;
import com.board.manager.request.CreateTaskRequest;
import com.board.manager.request.UpdateTaskRequest;
import com.board.manager.service.TaskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/board/{boardId}/task")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;


    @PostMapping
    public ResponseEntity<TaskDto> createTask(@PathVariable Integer boardId, @Valid @RequestBody CreateTaskRequest request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.createTask(boardId, request, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<TaskDto>> getTasks(@PathVariable Integer boardId) {
        return ResponseEntity.ok(taskService.getTasksByBoardId(boardId));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskDto> updateTask(@PathVariable Integer boardId, @PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.updateTask(boardId, taskId, request, currentUser));
    }

    @PatchMapping("/{taskId}/assign")
    public ResponseEntity<TaskDto> assignTask(@PathVariable Integer boardId, @PathVariable UUID taskId, @RequestBody Map<String, Integer> assignRequest, @AuthenticationPrincipal User currentUser) {
        Integer assigneeId = assignRequest.get("assigneeId");
        return ResponseEntity.ok(taskService.assignTask(boardId, taskId, assigneeId, currentUser));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskDto> updateTaskStatus(@PathVariable Integer boardId, @PathVariable UUID taskId, @RequestBody Map<String, String> statusRequest, @AuthenticationPrincipal User currentUser) {
        String status = statusRequest.get("status");
        return ResponseEntity.ok(taskService.updateTaskStatus(boardId, taskId, status, currentUser));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Integer boardId, @PathVariable UUID taskId, @AuthenticationPrincipal User currentUser) {
        taskService.deleteTask(boardId, taskId, currentUser);
        return ResponseEntity.noContent().build();
    }
}