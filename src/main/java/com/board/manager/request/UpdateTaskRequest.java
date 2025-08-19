package com.board.manager.request;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateTaskRequest {

    private String title;
    private String description;
    
    @Pattern(regexp = "TODO|IN_PROGRESS|DONE",
             message = "Status must be one of TODO, IN_PROGRESS, DONE")
    private String status;
    
    private Integer assignedTo;
}
