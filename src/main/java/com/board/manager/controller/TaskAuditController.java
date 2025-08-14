package com.board.manager.controller;

import com.board.manager.dto.TaskDto;
import com.board.manager.service.TaskAuditService;
import lombok.RequiredArgsConstructor;
import org.javers.core.diff.Change;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tasks/{taskId}/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TaskAuditController {

    private final TaskAuditService taskAuditService;

    @GetMapping("/changes")
    public ResponseEntity<List<Change>> getTaskChanges(@PathVariable UUID taskId) {
        List<Change> changes = taskAuditService.getTaskChanges(taskId);
        return ResponseEntity.ok(changes);
    }
}