package com.board.manager.mapper;

import com.board.manager.dto.TaskDto;
import com.board.manager.model.Task;
import com.board.manager.model.User;
import com.board.manager.request.CreateTaskRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(source = "board.id", target = "boardId")
    TaskDto toDto(Task task);

    @Mapping(source = "boardId", target = "board.id")
    Task toEntity(TaskDto taskDto);

    @Mapping(target = "assignedTo", ignore = true)
    Task toEntity(CreateTaskRequest request);
}