package com.board.manager.exception.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
public class ErrorDetails {
    private boolean success = false;
    private LocalDateTime timestamp;
    private String message;
    private String details;
    private HttpStatus httpStatus;

    public ErrorDetails(LocalDateTime timestamp, String message, String details, HttpStatus httpStatus) {
        this.timestamp = timestamp;
        this.message = message;
        this.details = details;
        this.httpStatus = httpStatus;
    }
}
