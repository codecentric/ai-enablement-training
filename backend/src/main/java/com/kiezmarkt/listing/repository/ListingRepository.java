package com.kiezmarkt.listing.repository;

import com.kiezmarkt.listing.domain.Listing;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * In-memory, thread-safe store of listings. No database server backs this
 * service; the seed set is loaded here at startup.
 */
@Repository
public class ListingRepository {

    private final Map<String, Listing> byId = new ConcurrentHashMap<>();

    public void save(Listing listing) {
        byId.put(listing.getId(), listing);
    }

    public Optional<Listing> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<Listing> findAll() {
        return byId.values();
    }

    public boolean existsById(String id) {
        return byId.containsKey(id);
    }

    public String nextId() {
        String candidate;
        do {
            candidate = "lst_" + randomHex();
        } while (byId.containsKey(candidate));
        return candidate;
    }

    private String randomHex() {
        int value = ThreadLocalRandom.current().nextInt(0, 0x1000000);
        return String.format("%06x", value);
    }
}
