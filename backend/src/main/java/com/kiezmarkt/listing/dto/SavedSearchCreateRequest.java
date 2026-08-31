package com.kiezmarkt.listing.dto;

import com.kiezmarkt.listing.domain.SavedSearchFilters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /saved-searches}. {@code id}, {@code sellerId} and
 * {@code lastMatchedAt} are server-owned and absent by design; sending them
 * is an unrecognized property and fails with {@code 400}.
 */
public class SavedSearchCreateRequest {

    @NotBlank
    @Size(min = 1, max = 80)
    private String label;

    @Size(max = 200)
    private String query = "";

    @Valid
    private SavedSearchFilters filters = new SavedSearchFilters();

    private boolean notify = false;

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
}
