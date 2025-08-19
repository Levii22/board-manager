package com.board.manager.dto;

import com.board.manager.model.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardUpdateDto {
    
    public enum UpdateType {
        TASK_CREATED, TASK_UPDATED, TASK_DELETED, TASK_ASSIGNED, 
        USER_JOINED, USER_LEFT, BOARD_UPDATED
    }
    
    private UpdateType type;
    private Integer boardId;
    private UUID taskId;
    private TaskDto taskData;
    private Integer userId;
    private String username;
    private String message;
    private LocalDateTime timestamp;
}
