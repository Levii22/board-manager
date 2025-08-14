package com.board.manager.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateBoardRequest {
    @NotEmpty(message = "name cannot be empty")
    private String name;
}