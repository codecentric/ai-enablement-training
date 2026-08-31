package com.kiezmarkt.listing.error;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ProblemException {
    public NotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, ProblemTypes.NOT_FOUND, "Not found", detail);
    }
}
