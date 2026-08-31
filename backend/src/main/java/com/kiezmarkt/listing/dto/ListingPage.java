package com.kiezmarkt.listing.dto;

import com.kiezmarkt.listing.domain.Listing;

import java.util.List;

/**
 * One page of search results. {@code nextCursor} is {@code null} on the
 * last page.
 */
public class ListingPage {

    private final List<Listing> items;
    private final String nextCursor;

    public ListingPage(List<Listing> items, String nextCursor) {
        this.items = items;
        this.nextCursor = nextCursor;
    }

    public List<Listing> getItems() {
        return items;
    }

    public String getNextCursor() {
        return nextCursor;
    }
}
