package com.board.manager.controller;

import com.board.manager.dto.BoardMemberDto;
import com.board.manager.model.User;
import com.board.manager.request.CreateUserRequest;
import com.board.manager.service.BoardMemberService;
import com.board.manager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/user")
class UserController {

    private final UserService userService;
    private final BoardMemberService boardMemberService;

    @PostMapping("/")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            userService.createUser(request.getUsername(), request.getPassword(), request.getEmail(), request.getRole());
            return ResponseEntity.ok().body("User created successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{username}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable String username,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String newEmail,
            @RequestParam(required = false) String newRole
    ) {
        try {
            userService.updateUser(username, newPassword, newEmail, newRole);
            return ResponseEntity.ok().body("User updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{username}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        try {
            userService.deleteUser(username);
            return ResponseEntity.ok().body("User deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/memberships")
    @Operation(summary = "Get user's board memberships", description = "Get all boards the current user is a member of")
    public ResponseEntity<List<BoardMemberDto>> getUserBoardMemberships(
            @AuthenticationPrincipal User currentUser) {

        List<BoardMemberDto> memberships = boardMemberService.getUserBoardMemberships(currentUser);
        return ResponseEntity.ok(memberships);
    }
}