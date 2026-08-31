package com.kiezmarkt.listing.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * A seller, as stored internally. {@code email} is PII and lives only here —
 * it must never be copied onto any DTO returned by the API. See
 * {@link com.kiezmarkt.listing.dto.SellerPublic}, the only shape a seller is
 * ever served in.
 */
public class Seller {

    private String id;
    private String displayName;
    private String email;
    private Instant createdAt;
    private boolean isCommercial;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @JsonProperty("isCommercial")
    public boolean isCommercial() {
        return isCommercial;
    }

    @JsonProperty("isCommercial")
    public void setCommercial(boolean commercial) {
        isCommercial = commercial;
    }
}
