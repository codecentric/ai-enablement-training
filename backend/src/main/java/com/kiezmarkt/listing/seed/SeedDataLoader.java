package com.kiezmarkt.listing.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiezmarkt.listing.repository.CategoryRepository;
import com.kiezmarkt.listing.repository.ListingRepository;
import com.kiezmarkt.listing.repository.SavedSearchRepository;
import com.kiezmarkt.listing.repository.SellerRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Loads {@code seed.json} into the in-memory repositories at startup. There
 * is no database server behind this service; this is the whole dataset.
 */
@Component
public class SeedDataLoader implements ApplicationRunner {

    private final ObjectMapper objectMapper;
    private final SellerRepository sellerRepository;
    private final CategoryRepository categoryRepository;
    private final ListingRepository listingRepository;
    private final SavedSearchRepository savedSearchRepository;

    public SeedDataLoader(ObjectMapper objectMapper,
                           SellerRepository sellerRepository,
                           CategoryRepository categoryRepository,
                           ListingRepository listingRepository,
                           SavedSearchRepository savedSearchRepository) {
        this.objectMapper = objectMapper;
        this.sellerRepository = sellerRepository;
        this.categoryRepository = categoryRepository;
        this.listingRepository = listingRepository;
        this.savedSearchRepository = savedSearchRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (InputStream in = new ClassPathResource("seed.json").getInputStream()) {
            SeedData seed = objectMapper.readValue(in, SeedData.class);
            seed.getSellers().forEach(sellerRepository::save);
            seed.getCategories().forEach(categoryRepository::save);
            seed.getListings().forEach(listingRepository::save);
            seed.getSavedSearches().forEach(savedSearchRepository::save);
        }
    }
}
