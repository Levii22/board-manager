package com.board.manager.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class TaskDto {
    private UUID id;
    private String title;
    private Integer boardId;
    private String description;
    private UserSummaryDto owner;
    private UserSummaryDto assignedTo;
    private String status;
}