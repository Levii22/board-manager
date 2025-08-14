package com.board.manager.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateTaskRequest {

    @NotEmpty(message = "title cannot be empty")
    private String title;

    @NotEmpty(message = "description cannot be empty")
    private String description;

    @NotEmpty(message = "Status cannot be empty")
    @Pattern(regexp = "TODO|IN_PROGRESS|DONE",
             message = "Status must be one of TODO, IN_PROGRESS, DONE")
    private String status;

    private Integer assignedTo;
}