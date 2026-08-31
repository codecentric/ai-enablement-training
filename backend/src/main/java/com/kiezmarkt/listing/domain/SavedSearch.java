package com.kiezmarkt.listing.domain;

import java.time.Instant;

/**
 * A saved search, as stored internally and as served by the API — the API
 * schema hides nothing about a saved search.
 */
public class SavedSearch {

    private String id;
    private String sellerId;
    private String label;
    private String query;
    private SavedSearchFilters filters = new SavedSearchFilters();
    private boolean notify;
    private Instant lastMatchedAt;

    public SavedSearch copy() {
        SavedSearch copy = new SavedSearch();
        copy.id = this.id;
        copy.sellerId = this.sellerId;
        copy.label = this.label;
        copy.query = this.query;
        copy.filters = this.filters;
        copy.notify = this.notify;
        copy.lastMatchedAt = this.lastMatchedAt;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public SavedSearchFilters getFilters() {
        return filters;
    }

    public void setFilters(SavedSearchFilters filters) {
        this.filters = filters == null ? new SavedSearchFilters() : filters;
    }

    public boolean isNotify() {
        return notify;
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    public Instant getLastMatchedAt() {
        return lastMatchedAt;
    }

    public void setLastMatchedAt(Instant lastMatchedAt) {
        this.lastMatchedAt = lastMatchedAt;
    }
}
