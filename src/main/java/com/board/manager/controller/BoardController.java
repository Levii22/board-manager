package com.board.manager.controller;

import com.board.manager.dto.BoardDto;
import com.board.manager.request.CreateBoardRequest;
import com.board.manager.model.User;
import com.board.manager.service.BoardService;
import com.board.manager.service.WebSocketConnectionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/board")
@SecurityRequirement(name = "bearerAuth")
public class BoardController {

    public final BoardService boardService;
    public final WebSocketConnectionManager connectionManager;

    @GetMapping
    public ResponseEntity<List<BoardDto>> getUserBoards(@AuthenticationPrincipal User currentUser) {
        List<BoardDto> boards = boardService.findBoardsByUser(currentUser);
        return ResponseEntity.ok(boards);
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardDto> getBoard(@PathVariable Integer boardId, @AuthenticationPrincipal User currentUser) {

        if (!boardService.canUserAccessBoard(boardId, currentUser)) {
            throw new AccessDeniedException("You do not have permission to access this resource.");
        }

        return boardService.findById(boardId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping()
    @Operation(summary = "Create board", description = "Create a new board")
    public ResponseEntity<BoardDto> createBoard(@Valid @RequestBody CreateBoardRequest request, @AuthenticationPrincipal User currentUser) {
        try {
            BoardDto createdBoard = boardService.createBoard(request.getName(), currentUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBoard);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{boardId}")
    @Operation(summary = "Delete board", description = "Delete a board by ID")
    public ResponseEntity<Void> deleteBoard(@PathVariable Integer boardId, @AuthenticationPrincipal User currentUser) {
        if (!boardService.canUserAccessBoard(boardId, currentUser)) {
            throw new AccessDeniedException("You do not have permission to delete this board.");
        }

        boardService.deleteBoard(boardId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{boardId}/ws-users")
    @Operation(summary = "Get active users on board", description = "Get list of users currently connected to a specific board")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Set<String>> getActiveBoardUsers(@PathVariable Integer boardId) {
        Set<String> activeUsers = connectionManager.getActiveBoardUsers(boardId);
        return ResponseEntity.ok(activeUsers);
    }
}