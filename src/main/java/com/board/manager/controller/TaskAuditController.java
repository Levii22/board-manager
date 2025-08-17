package com.board.manager.controller;

import com.board.manager.service.TaskAuditService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "bearerAuth")
public class TaskAuditController {

    private final TaskAuditService taskAuditService;

    @GetMapping("/changes")
    public ResponseEntity<List<Change>> getTaskChanges(@PathVariable UUID taskId) {
        List<Change> changes = taskAuditService.getTaskChanges(taskId);
        return ResponseEntity.ok(changes);
    }
}