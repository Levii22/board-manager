package com.board.manager.service;

import com.board.manager.dto.BoardDto;
import com.board.manager.model.User;

import java.util.List;
import java.util.Optional;

public interface BoardService {
    BoardDto createBoard(String name, User owner);
    boolean canUserAccessBoard(Integer boardId, User user);
    List<BoardDto> findBoardsByUser(User user);
    void deleteBoard(Integer boardId, User user);
    Optional<BoardDto> findById(Integer boardId);
}
