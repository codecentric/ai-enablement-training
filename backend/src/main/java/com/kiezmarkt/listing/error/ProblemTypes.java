package com.kiezmarkt.listing.error;

/**
 * The problem-type URI prefix used across this service, per {@code contracts/api.yaml}.
 */
public final class ProblemTypes {

    public static final String BASE = "https://kiezmarkt.example/problems/";

    public static final String VALIDATION_FAILED = BASE + "validation-failed";
    public static final String RADIUS_WITHOUT_POSTCODE = BASE + "radius-without-postcode";
    public static final String INVALID_CURSOR = BASE + "invalid-cursor";
    public static final String NOT_FOUND = BASE + "not-found";
    public static final String LISTING_DELETED = BASE + "listing-deleted";
    public static final String TERMINAL_STATUS = BASE + "terminal-status";
    public static final String ILLEGAL_STATUS_TRANSITION = BASE + "illegal-status-transition";
    public static final String CATEGORY_NOT_A_LEAF = BASE + "category-not-a-leaf";
    public static final String CATEGORY_NOT_FOUND = BASE + "category-not-found";
    public static final String SELLER_NOT_FOUND = BASE + "seller-not-found";

    private ProblemTypes() {
    }
}
