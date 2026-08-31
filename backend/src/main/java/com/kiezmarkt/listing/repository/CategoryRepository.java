package com.kiezmarkt.listing.repository;

import com.kiezmarkt.listing.domain.Category;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CategoryRepository {

    private final Map<String, Category> byId = new ConcurrentHashMap<>();

    public void save(Category category) {
        byId.put(category.getId(), category);
    }

    public Optional<Category> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<Category> findAll() {
        return byId.values();
    }
}
