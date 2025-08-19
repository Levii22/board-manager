package com.board.manager.dto;

import com.board.manager.model.BoardMember;
import lombok.Data;

@Data
public class BoardMemberDto {
    private Integer id;
    private Integer boardId;
    private String boardName;
    private Integer userId;
    private String username;
    private String email;
    private BoardMember.BoardRole role;
    private String createdAt;
}
