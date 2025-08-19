package com.board.manager.repository;

import com.board.manager.model.Board;
import com.board.manager.model.BoardMember;
import com.board.manager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardMemberRepository extends JpaRepository<BoardMember, Integer> {
    
    Optional<BoardMember> findByBoardAndUser(Board board, User user);
    
    List<BoardMember> findByBoard(Board board);
    
    List<BoardMember> findByUser(User user);
    
    @Query("SELECT bm FROM BoardMember bm WHERE bm.board.id = :boardId AND bm.user.id = :userId")
    Optional<BoardMember> findByBoardIdAndUserId(@Param("boardId") Integer boardId, @Param("userId") Integer userId);
    
    @Query("SELECT bm FROM BoardMember bm WHERE bm.user.id = :userId")
    List<BoardMember> findAllByUserId(@Param("userId") Integer userId);
    
    boolean existsByBoardAndUser(Board board, User user);
    
    void deleteByBoardAndUser(Board board, User user);
}
