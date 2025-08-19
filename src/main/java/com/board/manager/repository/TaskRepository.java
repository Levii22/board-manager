package com.board.manager.repository;

import com.board.manager.model.Task;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JaversSpringDataAuditable
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByBoardId(Integer boardId);
    void deleteByIdAndBoardId(UUID id, Integer boardId);
    Optional<Task> findByIdAndBoardId(UUID id, Integer boardId);
}