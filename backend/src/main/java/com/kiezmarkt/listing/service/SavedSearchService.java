package com.kiezmarkt.listing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiezmarkt.listing.domain.SavedSearch;
import com.kiezmarkt.listing.domain.SavedSearchFilters;
import com.kiezmarkt.listing.dto.SavedSearchCreateRequest;
import com.kiezmarkt.listing.error.NotFoundException;
import com.kiezmarkt.listing.error.RadiusWithoutPostcodeException;
import com.kiezmarkt.listing.error.ValidationFailedException;
import com.kiezmarkt.listing.repository.SavedSearchRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class SavedSearchService {

    /**
     * {@code domain.md} defines no authentication model, so nothing in the
     * request establishes who the caller is. {@code SavedSearchCreate}
     * carries no {@code sellerId} either. Every saved search created through
     * this service is attributed to this placeholder account until an auth
     * mechanism exists to say otherwise.
     */
    private static final String CALLER_SELLER_ID = "sel_0000";

    private static final Set<String> PATCHABLE_FIELDS = Set.of("label", "query", "filters", "notify");
    private static final Set<String> SERVER_OWNED_FIELDS = Set.of("id", "sellerId", "lastMatchedAt");

    private final SavedSearchRepository savedSearchRepository;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public SavedSearchService(SavedSearchRepository savedSearchRepository,
                               CategoryService categoryService,
                               ObjectMapper objectMapper,
                               Validator validator) {
        this.savedSearchRepository = savedSearchRepository;
        this.categoryService = categoryService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public List<SavedSearch> list() {
        return List.copyOf(savedSearchRepository.findAll());
    }

    public SavedSearch create(SavedSearchCreateRequest request) {
        SavedSearchFilters filters = request.getFilters() == null ? new SavedSearchFilters() : request.getFilters();
        validateFilters(filters);

        SavedSearch savedSearch = new SavedSearch();
        savedSearch.setId(savedSearchRepository.nextId());
        savedSearch.setSellerId(CALLER_SELLER_ID);
        savedSearch.setLabel(request.getLabel());
        savedSearch.setQuery(request.getQuery() == null ? "" : request.getQuery());
        savedSearch.setFilters(filters);
        savedSearch.setNotify(request.isNotify());
        savedSearch.setLastMatchedAt(null);

        savedSearchRepository.save(savedSearch);
        return savedSearch;
    }

    public SavedSearch patch(String id, JsonNode patch) {
        SavedSearch savedSearch = savedSearchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No saved search with id " + id + "."));
        if (patch == null || !patch.isObject() || patch.isEmpty()) {
            throw new ValidationFailedException("At least one property must be supplied.");
        }

        List<String> fields = new ArrayList<>();
        patch.fieldNames().forEachRemaining(fields::add);
        for (String field : fields) {
            if (SERVER_OWNED_FIELDS.contains(field)) {
                throw new ValidationFailedException(field + " is server-owned and cannot be patched.");
            }
            if (!PATCHABLE_FIELDS.contains(field)) {
                throw new ValidationFailedException("Unknown property: " + field);
            }
            if (patch.get(field).isNull()) {
                throw new ValidationFailedException(field + " must not be null.");
            }
        }

        SavedSearch updated = savedSearch.copy();

        if (patch.has("label")) {
            String label = textValue(patch, "label");
            if (label.isEmpty() || label.length() > 80) {
                throw new ValidationFailedException("label must be between 1 and 80 characters.");
            }
            updated.setLabel(label);
        }
        if (patch.has("query")) {
            String query = textValue(patch, "query");
            if (query.length() > 200) {
                throw new ValidationFailedException("query must be at most 200 characters.");
            }
            updated.setQuery(query);
        }
        if (patch.has("notify")) {
            JsonNode node = patch.get("notify");
            if (!node.isBoolean()) {
                throw new ValidationFailedException("notify must be a boolean.");
            }
            updated.setNotify(node.asBoolean());
        }
        if (patch.has("filters")) {
            JsonNode node = patch.get("filters");
            if (!node.isObject()) {
                throw new ValidationFailedException("filters must be an object.");
            }
            SavedSearchFilters filters;
            try {
                filters = objectMapper.treeToValue(node, SavedSearchFilters.class);
            } catch (Exception e) {
                throw new ValidationFailedException("filters is malformed.");
            }
            validateFilters(filters);
            // Replaced wholesale, not merged (contracts/api.yaml).
            updated.setFilters(filters);
        }

        savedSearchRepository.save(updated);
        return updated;
    }

    public void delete(String id) {
        if (!savedSearchRepository.existsById(id)) {
            throw new NotFoundException("No saved search with id " + id + ".");
        }
        savedSearchRepository.deleteById(id);
    }

    private void validateFilters(SavedSearchFilters filters) {
        Set<ConstraintViolation<SavedSearchFilters>> violations = validator.validate(filters);
        if (!violations.isEmpty()) {
            ConstraintViolation<SavedSearchFilters> first = violations.iterator().next();
            throw new ValidationFailedException("filters." + first.getPropertyPath() + " " + first.getMessage());
        }
        if (filters.getRadiusKm() != null && filters.getPostcode() == null) {
            throw new RadiusWithoutPostcodeException();
        }
        if (filters.getCategoryId() != null) {
            categoryService.requireKnownCategory(filters.getCategoryId());
        }
    }

    private String textValue(JsonNode patch, String field) {
        JsonNode value = patch.get(field);
        if (!value.isTextual()) {
            throw new ValidationFailedException(field + " must be a string.");
        }
        return value.asText();
    }
}
