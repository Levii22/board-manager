package com.board.manager.service;

import com.board.manager.dto.TaskDto;
import org.javers.core.diff.Change;

import java.util.List;
import java.util.UUID;

public interface TaskAuditService {

    List<Change> getTaskChanges(UUID taskId);
}