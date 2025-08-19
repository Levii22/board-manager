package com.board.manager.mapper;

import com.board.manager.dto.BoardMemberDto;
import com.board.manager.dto.MemberDto;
import com.board.manager.model.BoardMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BoardMemberMapper {

    @Mapping(source = "board.id", target = "boardId")
    @Mapping(source = "board.name", target = "boardName")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "email")
    BoardMemberDto toDto(BoardMember boardMember);

    List<BoardMemberDto> toDtoList(List<BoardMember> boardMembers);

    // For list of members in a board
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "email")
    MemberDto toMemberDto(BoardMember boardMember);

    List<MemberDto> toMemberDtoList(List<BoardMember> boardMembers);
}
