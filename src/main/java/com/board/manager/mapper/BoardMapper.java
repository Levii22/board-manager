package com.board.manager.mapper;

import com.board.manager.dto.BoardDto;
import com.board.manager.model.Board;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = TaskMapper.class)
public interface BoardMapper {
    BoardDto toDto(Board board);
    List<BoardDto> toDtoList(List<Board> boards);
}