package com.kiezmarkt.listing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Body of {@code POST /listings}. {@code status} and {@code publishedAt} are
 * deliberately absent: a new listing is always a {@code draft} with
 * {@code publishedAt: null}. Sending either field is an unrecognized
 * property and fails deserialization with a {@code 400} before this class is
 * even populated.
 */
public class ListingCreateRequest {

    @NotBlank
    @Size(min = 1, max = 64)
    private String sellerId;

    @NotBlank
    @Size(min = 3, max = 80)
    private String title;

    @Size(max = 4000)
    private String description = "";

    @NotBlank
    @Pattern(regexp = Patterns.CATEGORY_ID)
    private String categoryId;

    @NotNull
    @Min(0)
    private Long priceCents;

    @Pattern(regexp = Patterns.CURRENCY)
    private String currency = "EUR";

    @NotBlank
    @Pattern(regexp = Patterns.POSTCODE)
    private String postcode;

    @Size(max = 20)
    private List<@Size(min = 1, max = 64) String> imageIds = new ArrayList<>();

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

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public List<String> getImageIds() {
        return imageIds;
    }

    public void setImageIds(List<String> imageIds) {
        this.imageIds = imageIds == null ? new ArrayList<>() : imageIds;
    }
}
