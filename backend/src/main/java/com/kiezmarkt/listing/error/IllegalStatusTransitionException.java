package com.kiezmarkt.listing.error;

import org.springframework.http.HttpStatus;

public class IllegalStatusTransitionException extends ProblemException {
    public IllegalStatusTransitionException(String detail) {
        super(HttpStatus.CONFLICT, ProblemTypes.ILLEGAL_STATUS_TRANSITION, "Illegal status transition", detail);
    }
}
