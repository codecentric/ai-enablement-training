package com.kiezmarkt.listing.error;

import org.springframework.http.HttpStatus;

public class ValidationFailedException extends ProblemException {
    public ValidationFailedException(String detail) {
        super(HttpStatus.BAD_REQUEST, ProblemTypes.VALIDATION_FAILED, "Validation failed", detail);
    }
}
