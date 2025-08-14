package com.board.manager.repository;

import com.board.manager.model.Board;
import com.board.manager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Integer> {
    List<Board> findByOwner(User user);
}