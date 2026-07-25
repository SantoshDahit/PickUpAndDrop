package com.pickupdrop.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ErrorResponse {

    private final String message;
    private final String errorCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    public ErrorResponse(ErrorCode errorCode) {
        this.message = errorCode.getMessage();
        this.errorCode = errorCode.getErrorCode();
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String message) {
        this.message = message;
        this.errorCode = "";
        this.timestamp = LocalDateTime.now();
    }
}
