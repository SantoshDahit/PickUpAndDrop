package com.landgreet.user;

public class InvalidImageException extends RuntimeException {

    public InvalidImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
