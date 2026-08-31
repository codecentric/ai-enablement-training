package com.kiezmarkt.listing.repository;

import com.kiezmarkt.listing.domain.SavedSearch;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Repository
public class SavedSearchRepository {

    private final Map<String, SavedSearch> byId = new ConcurrentHashMap<>();

    public void save(SavedSearch savedSearch) {
        byId.put(savedSearch.getId(), savedSearch);
    }

    public Optional<SavedSearch> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<SavedSearch> findAll() {
        return byId.values();
    }

    public void deleteById(String id) {
        byId.remove(id);
    }

    public boolean existsById(String id) {
        return byId.containsKey(id);
    }

    public String nextId() {
        String candidate;
        do {
            candidate = "ss_" + randomHex();
        } while (byId.containsKey(candidate));
        return candidate;
    }

    private String randomHex() {
        int value = ThreadLocalRandom.current().nextInt(0, 0x1000000);
        return String.format("%06x", value);
    }
}
