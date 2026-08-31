package com.kiezmarkt.listing.repository;

import com.kiezmarkt.listing.domain.Seller;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SellerRepository {

    private final Map<String, Seller> byId = new ConcurrentHashMap<>();

    public void save(Seller seller) {
        byId.put(seller.getId(), seller);
    }

    public Optional<Seller> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public boolean existsById(String id) {
        return byId.containsKey(id);
    }
}
