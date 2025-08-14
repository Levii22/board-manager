package com.board.manager.service;

import com.board.manager.model.User;

public interface UserService {
    User createUser(String username, String password, String email, String role);
    boolean updateUser(String username, String newPassword, String newEmail, String newRole);
    void deleteUser(String username);
}
