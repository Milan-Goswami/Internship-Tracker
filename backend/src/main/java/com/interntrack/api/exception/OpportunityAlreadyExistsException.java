package com.interntrack.api.exception;

public class OpportunityAlreadyExistsException extends RuntimeException {

    public OpportunityAlreadyExistsException(String message) {
        super(message);
    }
}