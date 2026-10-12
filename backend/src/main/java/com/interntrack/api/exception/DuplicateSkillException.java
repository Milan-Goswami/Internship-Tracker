package com.interntrack.api.exception;

public class DuplicateSkillException extends RuntimeException {
    public DuplicateSkillException(String message) {
        super(message);
    }
}