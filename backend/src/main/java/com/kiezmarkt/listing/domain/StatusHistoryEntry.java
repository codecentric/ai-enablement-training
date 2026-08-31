package com.kiezmarkt.listing.domain;

import java.time.Instant;

/**
 * A single accepted status transition, kept in memory. Rejected transitions
 * record nothing. Not exposed through the API — {@code domain.md} does not
 * define a history endpoint, this is bookkeeping only.
 */
public class StatusHistoryEntry {

    private final String listingId;
    private final ListingStatus from;
    private final ListingStatus to;
    private final String reason;
    private final Instant changedAt;

    public StatusHistoryEntry(String listingId, ListingStatus from, ListingStatus to, String reason, Instant changedAt) {
        this.listingId = listingId;
        this.from = from;
        this.to = to;
        this.reason = reason;
        this.changedAt = changedAt;
    }

    public String getListingId() {
        return listingId;
    }

    public ListingStatus getFrom() {
        return from;
    }

    public ListingStatus getTo() {
        return to;
    }

    public String getReason() {
        return reason;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
