package com.board.manager.service;

import com.board.manager.dto.BoardDto;
import com.board.manager.mapper.BoardMapper;
import com.board.manager.model.Board;
import com.board.manager.model.User;
import com.board.manager.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final BoardMapper boardMapper;

    @Caching(evict = {
        @CacheEvict(value = "boards", allEntries = true),
        @CacheEvict(value = "board", key = "#result.id", condition = "#result != null")
    })
    public BoardDto createBoard(String name, User owner) {
        Board board = new Board();
        board.setName(name.trim());
        board.setOwner(owner);
        Board savedBoard = boardRepository.save(board);
        log.debug("Created board with ID: {} and invalidated cache", savedBoard.getId());
        return boardMapper.toDto(savedBoard);
    }

    @Transactional(readOnly = true)
    public boolean canUserAccessBoard(Integer boardId, User user) {
        log.debug("Checking access for user: {} to board: {}", user.getUsername(), boardId);

        if (user.getRole() == User.Role.ADMIN) {
            return true;
        }

        return boardRepository.findById(boardId)
                .map(board -> board.getOwner().getId().equals(user.getId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "boards", key = "#user.id + '_' + #user.role.name()")
    public List<BoardDto> findBoardsByUser(User user) {
        log.debug("Finding boards for user: {} (cache miss)", user.getUsername());

        List<Board> boards;
        if (user.getRole() == User.Role.ADMIN) {
            // Admins can see all boards
            boards = boardRepository.findAll();
        } else {
            // Members can only see their own boards
            boards = boardRepository.findByOwner(user);
        }
        return boards.stream()
                .map(boardMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "boards", allEntries = true),
        @CacheEvict(value = "board", key = "#boardId"),
        @CacheEvict(value = "tasks", key = "'board:' + #boardId")
    })
    public void deleteBoard(Integer boardId, User user) {
        log.debug("Deleting board with ID: {} for user: {} and invalidating cache", boardId, user.getUsername());
        boardRepository.deleteById(boardId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "board", key = "#boardId")
    public Optional<BoardDto> findById(Integer boardId) {
        log.debug("Finding board by ID: {} (cache miss)", boardId);
        return boardRepository.findById(boardId)
                .map(boardMapper::toDto);
    }
}