package com.kiezmarkt.listing.repository;

import com.kiezmarkt.listing.domain.StatusHistoryEntry;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Every accepted listing status transition, in memory. Rejected transitions
 * record nothing (see {@code POST /listings/{id}/status} in
 * {@code contracts/api.yaml}).
 */
@Repository
public class StatusHistoryRepository {

    private final List<StatusHistoryEntry> entries = new CopyOnWriteArrayList<>();

    public void record(StatusHistoryEntry entry) {
        entries.add(entry);
    }

    public List<StatusHistoryEntry> findByListingId(String listingId) {
        return entries.stream().filter(e -> e.getListingId().equals(listingId)).toList();
    }

    public List<StatusHistoryEntry> findAll() {
        return List.copyOf(entries);
    }
}
