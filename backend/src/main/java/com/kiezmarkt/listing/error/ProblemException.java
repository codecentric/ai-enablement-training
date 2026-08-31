package com.kiezmarkt.listing.error;

import org.springframework.http.HttpStatus;

/**
 * Base type for every domain-level error that maps to an RFC 9457 problem
 * response. Subclasses fix the status, {@code type} URI and title; only
 * {@code detail} varies per occurrence.
 */
public abstract class ProblemException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String title;

    protected ProblemException(HttpStatus status, String type, String title, String detail) {
        super(detail);
        this.status = status;
        this.type = type;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return getMessage();
    }
}
