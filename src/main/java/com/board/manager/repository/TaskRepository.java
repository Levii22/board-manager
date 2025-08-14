package com.board.manager.repository;

import com.board.manager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByBoardId(Integer boardId);
    void deleteByIdAndBoardId(UUID id, Integer boardId);
}