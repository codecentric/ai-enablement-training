package com.kiezmarkt.listing.domain;

/**
 * Lifecycle status of a {@link Listing}. {@code deleted} is terminal.
 */
public enum ListingStatus {
    draft,
    published,
    paused,
    expired,
    deleted
}
