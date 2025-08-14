package com.board.manager.service;

import com.board.manager.model.User;

public interface AuthService {
    void registerUser(String username, String password, String email);
    User authenticateUser(String identifier, String password);
}
