package com.board.manager.controller;

import com.board.manager.dto.BoardDto;
import com.board.manager.request.CreateBoardRequest;
import com.board.manager.model.User;
import com.board.manager.service.BoardServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/board")
public class BoardController {

    public final BoardServiceImpl boardServiceImpl;

    @GetMapping
    public ResponseEntity<List<BoardDto>> getUserBoards(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        List<BoardDto> boards = boardServiceImpl.findBoardsByUser(currentUser);
        return ResponseEntity.ok(boards);
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardDto> getBoard(@PathVariable Integer boardId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        if (!boardServiceImpl.canUserAccessBoard(boardId, currentUser)) {
            throw new AccessDeniedException("You do not have permission to access this resource.");
        }

        return boardServiceImpl.findById(boardId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping()
    @Operation(summary = "Create board", description = "Create a new board")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BoardDto> createBoard(@Valid @RequestBody CreateBoardRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        try {
            BoardDto createdBoard = boardServiceImpl.createBoard(request.getName(), currentUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBoard);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{boardId}")
    @Operation(summary = "Delete board", description = "Delete a board by ID")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteBoard(@PathVariable Integer boardId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        if (!boardServiceImpl.canUserAccessBoard(boardId, currentUser)) {
            throw new AccessDeniedException("You do not have permission to delete this board.");
        }

        boardServiceImpl.deleteBoard(boardId, currentUser);
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

}