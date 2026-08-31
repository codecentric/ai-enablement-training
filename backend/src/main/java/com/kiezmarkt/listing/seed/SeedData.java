package com.kiezmarkt.listing.seed;

import com.kiezmarkt.listing.domain.Category;
import com.kiezmarkt.listing.domain.Listing;
import com.kiezmarkt.listing.domain.SavedSearch;
import com.kiezmarkt.listing.domain.Seller;

import java.util.List;

/**
 * Shape of {@code seed.json}: {@code { sellers, categories, listings, savedSearches }}.
 */
public class SeedData {

    private List<Seller> sellers;
    private List<Category> categories;
    private List<Listing> listings;
    private List<SavedSearch> savedSearches;

    public List<Seller> getSellers() {
        return sellers;
    }

    public void setSellers(List<Seller> sellers) {
        this.sellers = sellers;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public List<Listing> getListings() {
        return listings;
    }

    public void setListings(List<Listing> listings) {
        this.listings = listings;
    }

    public List<SavedSearch> getSavedSearches() {
        return savedSearches;
    }

    public void setSavedSearches(List<SavedSearch> savedSearches) {
        this.savedSearches = savedSearches;
    }
}
