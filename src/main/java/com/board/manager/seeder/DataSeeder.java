package com.board.manager.seeder;

import com.board.manager.model.Board;
import com.board.manager.model.Task;
import com.board.manager.model.User;
import com.board.manager.repository.BoardRepository;
import com.board.manager.repository.UserRepository;
import com.board.manager.service.BoardServiceImpl;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final BoardServiceImpl boardServiceImpl;
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
        userRepository.save(user);

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@email.com");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setRole(User.Role.ADMIN);
        userRepository.save(admin);

        // Create board
        Board board = new Board();
        board.setName("Demo Board");
        board.setOwner(admin);
        board = boardRepository.save(board);

        // Create tasks
        Task task1 = new Task();
        task1.setTitle("First Task");
        task1.setDescription("This is the first task");
        task1.setStatus(Task.Status.TODO);
        task1.setOwner(admin);
        task1.setBoard(board);

        Task task2 = new Task();
        task2.setTitle("Second Task");
        task2.setDescription("This is the second task");
        task2.setStatus(Task.Status.IN_PROGRESS);
        task2.setOwner(user);
        task2.setBoard(board);

        board.getTasks().add(task1);
        board.getTasks().add(task2);
        boardRepository.save(board);
    }
}