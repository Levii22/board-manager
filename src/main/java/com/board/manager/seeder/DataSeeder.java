package com.board.manager.seeder;

import com.board.manager.dto.BoardDto;
import com.board.manager.model.User;
import com.board.manager.repository.UserRepository;
import com.board.manager.request.CreateTaskRequest;
import com.board.manager.service.BoardService;
import com.board.manager.service.TaskService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BoardService boardService;
    private final TaskService taskService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) return; // Prevent duplicate seeding

        // Create users
        User user = new User();
        user.setUsername("user");
        user.setEmail("user@email.com");
        user.setPassword(passwordEncoder.encode("userpass"));
        user.setRole(User.Role.MEMBER);
        user = userRepository.save(user);

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@email.com");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setRole(User.Role.ADMIN);
        admin = userRepository.save(admin);

        // Create board using service (this will automatically create BoardMember with OWNER role)
        BoardDto board = boardService.createBoard("Demo Board", admin);

        // Create tasks using service with builder pattern
        CreateTaskRequest task1Request = CreateTaskRequest.builder()
                .title("First Task")
                .description("This is the first task")
                .status("TODO")
                .assignedTo(admin.getId())
                .build();
        taskService.createTask(board.getId(), task1Request, admin);

        CreateTaskRequest task2Request = CreateTaskRequest.builder()
                .title("Second Task")
                .description("This is the second task")
                .status("IN_PROGRESS")
                .assignedTo(user.getId())
                .build();
        taskService.createTask(board.getId(), task2Request, admin);

        // create new board
        BoardDto newBoard = boardService.createBoard("New Board", user);
        // Create a task in the new board
        CreateTaskRequest newTaskRequest = CreateTaskRequest.builder()
                .title("New Task")
                .description("This is a new task in the new board")
                .status("TODO")
                .assignedTo(user.getId())
                .build();
        taskService.createTask(newBoard.getId(), newTaskRequest, user);
    }
}