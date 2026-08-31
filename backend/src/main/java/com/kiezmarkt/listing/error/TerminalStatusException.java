package com.kiezmarkt.listing.error;

import org.springframework.http.HttpStatus;

public class TerminalStatusException extends ProblemException {
    public TerminalStatusException(String detail) {
        super(HttpStatus.CONFLICT, ProblemTypes.TERMINAL_STATUS, "Listing is deleted", detail);
    }
}
