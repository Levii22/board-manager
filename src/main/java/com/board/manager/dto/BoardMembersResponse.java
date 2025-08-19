package com.board.manager.dto;

import lombok.Data;

import java.util.List;

@Data
public class BoardMembersResponse {
    private Integer boardId;
    private String boardName;
    private List<MemberDto> members;
}
