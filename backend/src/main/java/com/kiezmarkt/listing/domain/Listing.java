package com.kiezmarkt.listing.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A listing, both as stored internally and as served by the API — the API
 * schema hides nothing about a listing (unlike {@link Seller}, which hides
 * {@code email}), so one class serves both purposes.
 *
 * <p>{@code priceCents} is declared non-null in {@code domain.md} and
 * {@code contracts/api.yaml}, but the import feed delivers {@code null} for
 * listings posted without a price. This class models it as a nullable
 * {@link Long} on purpose: it must serialize as JSON {@code null}, never be
 * coerced to {@code 0} or omitted, and it must never become a {@code double}
 * or {@code float} anywhere in this path.
 */
public class Listing {

    private String id;
    private String sellerId;
    private String title;
    private String description;
    private String categoryId;
    private Long priceCents;
    private String currency;
    private ListingStatus status;
    private String postcode;
    private Instant createdAt;
    private Instant publishedAt;
    private List<String> imageIds = new ArrayList<>();

    public Listing() {
    }

    public Listing copy() {
        Listing copy = new Listing();
        copy.id = this.id;
        copy.sellerId = this.sellerId;
        copy.title = this.title;
        copy.description = this.description;
        copy.categoryId = this.categoryId;
        copy.priceCents = this.priceCents;
        copy.currency = this.currency;
        copy.status = this.status;
        copy.postcode = this.postcode;
        copy.createdAt = this.createdAt;
        copy.publishedAt = this.publishedAt;
        copy.imageIds = new ArrayList<>(this.imageIds);
        return copy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public Long getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(Long priceCents) {
        this.priceCents = priceCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public List<String> getImageIds() {
        return imageIds;
    }

    public void setImageIds(List<String> imageIds) {
        this.imageIds = imageIds == null ? new ArrayList<>() : imageIds;
    }
}
