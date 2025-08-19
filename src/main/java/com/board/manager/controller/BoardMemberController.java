package com.board.manager.controller;

import com.board.manager.dto.BoardMemberDto;
import com.board.manager.dto.BoardMembersResponse;
import com.board.manager.model.BoardMember;
import com.board.manager.model.User;
import com.board.manager.request.AddBoardMemberRequest;
import com.board.manager.service.BoardMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/board/{boardId}/members")
@RequiredArgsConstructor
@Tag(name = "Board Members", description = "Board member management operations")
public class BoardMemberController {

    private final BoardMemberService boardMemberService;

    @PostMapping
    @Operation(summary = "Add member to board", description = "Add a user to a board with specified role")
    public ResponseEntity<BoardMemberDto> addMemberToBoard(
            @PathVariable Integer boardId,
            @Valid @RequestBody AddBoardMemberRequest request,
            @AuthenticationPrincipal User currentUser) {

        BoardMemberDto member = boardMemberService.addMemberToBoard(
                boardId, request.getEmail(), request.getRole(), currentUser);
        return ResponseEntity.ok(member);
    }

    @GetMapping
    @Operation(summary = "Get board members", description = "Get all members of a board with optimized response structure")
    public ResponseEntity<BoardMembersResponse> getBoardMembers(
            @PathVariable Integer boardId,
            @AuthenticationPrincipal User currentUser) {

        BoardMembersResponse members = boardMemberService.getBoardMembers(boardId, currentUser);
        return ResponseEntity.ok(members);
    }

    @PutMapping("/{userId}/role")
    @Operation(summary = "Update member role", description = "Update the role of a board member")
    public ResponseEntity<BoardMemberDto> updateMemberRole(
            @PathVariable Integer boardId,
            @PathVariable Integer userId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User currentUser) {

        BoardMember.BoardRole newRole = BoardMember.BoardRole.valueOf(request.get("role"));
        BoardMemberDto updatedMember = boardMemberService.updateMemberRole(
                boardId, userId, newRole, currentUser);
        return ResponseEntity.ok(updatedMember);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Remove member from board", description = "Remove a user from a board")
    public ResponseEntity<Void> removeMemberFromBoard(
            @PathVariable Integer boardId,
            @PathVariable Integer userId,
            @AuthenticationPrincipal User currentUser) {

        boardMemberService.removeMemberFromBoard(boardId, userId, currentUser);
        return ResponseEntity.noContent().build();
    }
}