package com.board.manager.controller;

import com.board.manager.dto.TaskDto;
import com.board.manager.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/board/{boardId}/task")
public class TaskController {

    private final TaskService taskService;


    @PostMapping
    public ResponseEntity<TaskDto> createTask(@PathVariable Integer boardId, @RequestBody TaskDto taskDto) {
        return ResponseEntity.ok(taskService.createTask(boardId, taskDto));
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