package com.kiezmarkt.listing.dto;

/**
 * Regex patterns shared across request DTOs and query-parameter validation,
 * mirrored from {@code contracts/api.yaml}.
 */
public final class Patterns {

    public static final String CATEGORY_ID = "^[a-z0-9]+(\\.[a-z0-9]+)?$";
    public static final String POSTCODE = "^[0-9]{5}$";
    public static final String CURRENCY = "^[A-Z]{3}$";

    private Patterns() {
    }
}
