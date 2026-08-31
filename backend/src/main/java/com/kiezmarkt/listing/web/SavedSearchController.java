package com.kiezmarkt.listing.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiezmarkt.listing.domain.SavedSearch;
import com.kiezmarkt.listing.dto.SavedSearchCreateRequest;
import com.kiezmarkt.listing.service.SavedSearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/saved-searches")
public class SavedSearchController {

    private final SavedSearchService savedSearchService;

    public SavedSearchController(SavedSearchService savedSearchService) {
        this.savedSearchService = savedSearchService;
    }

    @GetMapping
    public List<SavedSearch> list() {
        return savedSearchService.list();
    }

    @PostMapping
    public ResponseEntity<SavedSearch> create(@Valid @RequestBody SavedSearchCreateRequest request) {
        SavedSearch created = savedSearchService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    public SavedSearch patch(@PathVariable String id, @RequestBody JsonNode patch) {
        return savedSearchService.patch(id, patch);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        savedSearchService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
