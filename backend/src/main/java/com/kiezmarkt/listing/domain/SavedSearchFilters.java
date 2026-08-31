package com.kiezmarkt.listing.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kiezmarkt.listing.dto.Patterns;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * The filter half of a saved search. Every field is optional; unset fields
 * are omitted from the serialized JSON rather than written as {@code null}
 * (unlike {@link Listing#getPriceCents()}, none of these properties is
 * declared nullable in the contract — they are simply absent-able).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SavedSearchFilters {

    @Pattern(regexp = Patterns.CATEGORY_ID)
    private String categoryId;

    @Min(0)
    private Long priceMinCents;

    @Min(0)
    private Long priceMaxCents;

    @Pattern(regexp = Patterns.POSTCODE)
    private String postcode;

    @Min(1)
    private Integer radiusKm;

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public Long getPriceMinCents() {
        return priceMinCents;
    }

    public void setPriceMinCents(Long priceMinCents) {
        this.priceMinCents = priceMinCents;
    }

    public Long getPriceMaxCents() {
        return priceMaxCents;
    }

    public void setPriceMaxCents(Long priceMaxCents) {
        this.priceMaxCents = priceMaxCents;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public Integer getRadiusKm() {
        return radiusKm;
    }

    public void setRadiusKm(Integer radiusKm) {
        this.radiusKm = radiusKm;
    }
}
