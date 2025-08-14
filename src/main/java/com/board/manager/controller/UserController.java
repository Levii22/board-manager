package com.board.manager.controller;

import com.board.manager.request.CreateUserRequest;
import com.board.manager.service.UserServiceImpl;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/user")
class UserController {

    public final UserServiceImpl userServiceImpl;

    @PostMapping("/")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            userServiceImpl.createUser(request.getUsername(), request.getPassword(), request.getEmail(), request.getRole());
            return ResponseEntity.ok().body("User created successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{username}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> updateUser(
            @PathVariable String username,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String newEmail,
            @RequestParam(required = false) String newRole
    ) {
        try {
            userServiceImpl.updateUser(username, newPassword, newEmail, newRole);
            return ResponseEntity.ok().body("User updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{username}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        try {
            userServiceImpl.deleteUser(username);
            return ResponseEntity.ok().body("User deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}