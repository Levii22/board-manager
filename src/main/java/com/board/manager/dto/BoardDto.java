package com.board.manager.dto;

import lombok.Data;
import java.util.List;

@Data
public class BoardDto {
    private Integer id;
    private String name;
    private List<TaskDto> tasks;
}