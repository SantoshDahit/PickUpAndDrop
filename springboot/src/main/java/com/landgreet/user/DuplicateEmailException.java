package com.landgreet.user;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("That email is already registered.");
    }
}
