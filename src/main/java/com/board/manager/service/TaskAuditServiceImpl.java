package com.board.manager.service;

import com.board.manager.mapper.TaskMapper;
import com.board.manager.model.Task;
import lombok.RequiredArgsConstructor;
import org.javers.core.Javers;
import org.javers.core.diff.Change;
import org.javers.repository.jql.QueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskAuditServiceImpl implements TaskAuditService {

    private final Javers javers;
    private final TaskMapper taskMapper;

    @Override
    public List<Change> getTaskChanges(UUID taskId) {
        return javers.findChanges(QueryBuilder.byInstanceId(taskId, Task.class).build());
    }
}