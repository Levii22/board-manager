package com.board.manager.service;

import com.board.manager.dto.BoardMemberDto;
import com.board.manager.dto.BoardMembersResponse;
import com.board.manager.dto.MemberDto;
import com.board.manager.mapper.BoardMemberMapper;
import com.board.manager.model.Board;
import com.board.manager.model.BoardMember;
import com.board.manager.model.User;
import com.board.manager.repository.BoardMemberRepository;
import com.board.manager.repository.BoardRepository;
import com.board.manager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardMemberService {

    private final BoardMemberRepository boardMemberRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final BoardMemberMapper boardMemberMapper;

    public BoardMemberDto addMemberToBoard(Integer boardId, String userEmail, BoardMember.BoardRole role, User currentUser) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException("Board not found"));

        // Check if current user has permission to add members (must be owner or admin)
        if (!hasAdminPermission(board, currentUser)) {
            throw new AccessDeniedException("You don't have permission to manage board members");
        }

        User userToAdd = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Check if user is already a member
        if (boardMemberRepository.existsByBoardAndUser(board, userToAdd)) {
            throw new IllegalArgumentException("User is already a member of this board");
        }

        // Cannot assign owner role to others
        if (role == BoardMember.BoardRole.OWNER) {
            throw new IllegalArgumentException("Cannot assign owner role to other users");
        }

        BoardMember boardMember = new BoardMember();
        boardMember.setBoard(board);
        boardMember.setUser(userToAdd);
        boardMember.setRole(role);

        BoardMember saved = boardMemberRepository.save(boardMember);
        return boardMemberMapper.toDto(saved);
    }

    public void removeMemberFromBoard(Integer boardId, Integer userId, User currentUser) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException("Board not found"));

        User userToRemove = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Check if current user has permission to remove members
        if (!hasAdminPermission(board, currentUser)) {
            throw new AccessDeniedException("You don't have permission to manage board members");
        }

        // Cannot remove the board owner
        if (board.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot remove board owner");
        }

        boardMemberRepository.deleteByBoardAndUser(board, userToRemove);
    }

    public BoardMemberDto updateMemberRole(Integer boardId, Integer userId, BoardMember.BoardRole newRole, User currentUser) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException("Board not found"));

        // Check if current user has permission to update roles
        if (!hasAdminPermission(board, currentUser)) {
            throw new AccessDeniedException("You don't have permission to manage board members");
        }

        BoardMember boardMember = boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Board member not found"));

        // Cannot change owner role or assign owner role to others
        if (boardMember.getRole() == BoardMember.BoardRole.OWNER || newRole == BoardMember.BoardRole.OWNER) {
            throw new IllegalArgumentException("Cannot modify owner role");
        }

        boardMember.setRole(newRole);
        BoardMember updated = boardMemberRepository.save(boardMember);
        return boardMemberMapper.toDto(updated);
    }

    public BoardMembersResponse getBoardMembers(Integer boardId, User currentUser) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException("Board not found"));

        // Check if current user has access to this board
        if (!hasAnyPermission(board, currentUser)) {
            throw new AccessDeniedException("You don't have access to this board");
        }

        List<BoardMember> members = boardMemberRepository.findByBoard(board);
        List<MemberDto> memberDtos = boardMemberMapper.toMemberDtoList(members);

        BoardMembersResponse response = new BoardMembersResponse();
        response.setBoardId(board.getId());
        response.setBoardName(board.getName());
        response.setMembers(memberDtos);

        return response;
    }

    public List<BoardMemberDto> getUserBoardMemberships(User user) {
        List<BoardMember> memberships = boardMemberRepository.findByUser(user);
        return boardMemberMapper.toDtoList(memberships);
    }

    public boolean hasPermission(Integer boardId, User user, BoardMember.BoardRole requiredRole) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException("Board not found"));

        // Board owner has all permissions
        if (board.getOwner().getId().equals(user.getId())) {
            return true;
        }

        Optional<BoardMember> membership = boardMemberRepository.findByBoardAndUser(board, user);
        if (membership.isEmpty()) {
            return false;
        }

        BoardMember.BoardRole userRole = membership.get().getRole();
        return hasRolePermission(userRole, requiredRole);
    }

    private boolean hasAdminPermission(Board board, User user) {
        // Board owner always has admin permission
        if (board.getOwner().getId().equals(user.getId())) {
            return true;
        }

        Optional<BoardMember> membership = boardMemberRepository.findByBoardAndUser(board, user);
        return membership.isPresent() &&
               (membership.get().getRole() == BoardMember.BoardRole.ADMIN);
    }

    private boolean hasAnyPermission(Board board, User user) {
        // Board owner always has access
        if (board.getOwner().getId().equals(user.getId())) {
            return true;
        }

        return boardMemberRepository.existsByBoardAndUser(board, user);
    }

    private boolean hasRolePermission(BoardMember.BoardRole userRole, BoardMember.BoardRole requiredRole) {
        // Define role hierarchy: OWNER > ADMIN > EDITOR > VIEWER
        return switch (requiredRole) {
            case VIEWER -> true; // All roles can view
            case EDITOR -> userRole == BoardMember.BoardRole.OWNER ||
                          userRole == BoardMember.BoardRole.ADMIN ||
                          userRole == BoardMember.BoardRole.EDITOR;
            case ADMIN -> userRole == BoardMember.BoardRole.OWNER ||
                         userRole == BoardMember.BoardRole.ADMIN;
            case OWNER -> userRole == BoardMember.BoardRole.OWNER;
        };
    }
}
