package com.board.manager.mapper;

import com.board.manager.dto.TaskDto;
import com.board.manager.dto.UserSummaryDto;
import com.board.manager.model.Task;
import com.board.manager.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(source = "board.id", target = "boardId")
    TaskDto toDto(Task task);

    @Mapping(source = "boardId", target = "board.id")
    Task toEntity(TaskDto taskDto);
}