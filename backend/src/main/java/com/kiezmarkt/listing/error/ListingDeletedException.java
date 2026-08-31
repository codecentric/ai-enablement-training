package com.kiezmarkt.listing.error;

import org.springframework.http.HttpStatus;

public class ListingDeletedException extends ProblemException {
    public ListingDeletedException(String detail) {
        super(HttpStatus.GONE, ProblemTypes.LISTING_DELETED, "Listing deleted", detail);
    }
}
