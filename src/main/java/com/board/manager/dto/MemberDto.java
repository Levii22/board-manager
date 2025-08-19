package com.board.manager.dto;

import com.board.manager.model.BoardMember;
import lombok.Data;

@Data
public class MemberDto {
    private Integer id;
    private Integer userId;
    private String username;
    private String email;
    private BoardMember.BoardRole role;
    private String createdAt;
}
