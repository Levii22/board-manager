package com.board.manager.service;

import com.board.manager.exception.InvalidRoleException;
import com.board.manager.model.User;
import com.board.manager.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String username, String password, String email, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        try {
            user.setRole(User.Role.valueOf(role));
        } catch (IllegalArgumentException e) {
            // TODO: Find a cleaner way to handle this
            throw new InvalidRoleException("Invalid role: " + role + ". Allowed values are: " +
                    String.join(", ", Arrays.stream(User.Role.values())
                            .map(Enum::name)
                            .toArray(String[]::new)));
        }
        return userRepository.save(user);
    }

    public boolean updateUser(String username, String newPassword, String newEmail, String newRole) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPassword(newPassword);
        }
        if (newEmail != null && !newEmail.isEmpty()) {
            if (userRepository.existsByEmail(newEmail)) {
                throw new IllegalArgumentException("Email already exists");
            }
            user.setEmail(newEmail);
        }
        if (newRole != null && !newRole.isEmpty()) {
            try {
                user.setRole(User.Role.valueOf(newRole));
            } catch (IllegalArgumentException e) {
                throw new InvalidRoleException("Invalid role: " + newRole + ". Allowed values are: " +
                        String.join(", ", Arrays.stream(User.Role.values())
                                .map(Enum::name)
                                .toArray(String[]::new)));
            }
        }

        userRepository.save(user);
        return true;
    }

    public void deleteUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        userRepository.delete(user);
    }
}