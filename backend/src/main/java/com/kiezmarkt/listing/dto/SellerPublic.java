package com.kiezmarkt.listing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The only shape a seller is ever served in. No {@code email} field exists
 * here — not filtered out, simply never present on this class (domain.md
 * invariant 6).
 */
public class SellerPublic {

    private final String id;
    private final String displayName;
    private final boolean isCommercial;

    public SellerPublic(String id, String displayName, boolean isCommercial) {
        this.id = id;
        this.displayName = displayName;
        this.isCommercial = isCommercial;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonProperty("isCommercial")
    public boolean isCommercial() {
        return isCommercial;
    }
}
