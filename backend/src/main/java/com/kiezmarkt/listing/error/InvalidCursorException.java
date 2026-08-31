package com.kiezmarkt.listing.error;

import org.springframework.http.HttpStatus;

public class InvalidCursorException extends ProblemException {
    public InvalidCursorException() {
        super(HttpStatus.BAD_REQUEST, ProblemTypes.INVALID_CURSOR, "Invalid cursor",
                "The cursor is malformed or no longer valid.");
    }
}
