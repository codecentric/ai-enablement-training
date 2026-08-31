package com.kiezmarkt.listing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiezmarkt.listing.domain.Listing;
import com.kiezmarkt.listing.domain.ListingStatus;
import com.kiezmarkt.listing.domain.StatusHistoryEntry;
import com.kiezmarkt.listing.dto.ListingCreateRequest;
import com.kiezmarkt.listing.dto.ListingPage;
import com.kiezmarkt.listing.dto.Patterns;
import com.kiezmarkt.listing.dto.StatusChangeTarget;
import com.kiezmarkt.listing.error.IllegalStatusTransitionException;
import com.kiezmarkt.listing.error.ListingDeletedException;
import com.kiezmarkt.listing.error.NotFoundException;
import com.kiezmarkt.listing.error.RadiusWithoutPostcodeException;
import com.kiezmarkt.listing.error.TerminalStatusException;
import com.kiezmarkt.listing.error.ValidationFailedException;
import com.kiezmarkt.listing.repository.ListingRepository;
import com.kiezmarkt.listing.repository.StatusHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ListingService {

    private static final Set<String> PATCHABLE_FIELDS =
            Set.of("title", "description", "categoryId", "priceCents", "currency", "postcode", "imageIds");
    private static final Set<String> SERVER_OWNED_FIELDS =
            Set.of("id", "sellerId", "createdAt", "publishedAt", "status");

    private static final Map<ListingStatus, Set<ListingStatus>> LEGAL_TRANSITIONS = buildTransitionTable();

    private final ListingRepository listingRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final CategoryService categoryService;
    private final SellerService sellerService;

    public ListingService(ListingRepository listingRepository,
                           StatusHistoryRepository statusHistoryRepository,
                           CategoryService categoryService,
                           SellerService sellerService) {
        this.listingRepository = listingRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.categoryService = categoryService;
        this.sellerService = sellerService;
    }

    private static Map<ListingStatus, Set<ListingStatus>> buildTransitionTable() {
        Map<ListingStatus, Set<ListingStatus>> table = new EnumMap<>(ListingStatus.class);
        table.put(ListingStatus.draft, EnumSet.of(ListingStatus.published, ListingStatus.deleted));
        table.put(ListingStatus.published, EnumSet.of(ListingStatus.paused, ListingStatus.expired, ListingStatus.deleted));
        table.put(ListingStatus.paused, EnumSet.of(ListingStatus.published, ListingStatus.expired, ListingStatus.deleted));
        table.put(ListingStatus.expired, EnumSet.of(ListingStatus.published, ListingStatus.deleted));
        table.put(ListingStatus.deleted, EnumSet.noneOf(ListingStatus.class));
        return table;
    }

    // ---- GET /listings ----------------------------------------------------

    public ListingPage search(String q, String categoryId, Long priceMinCents, Long priceMaxCents,
                               String postcode, Integer radiusKm, String cursor, int limit) {
        if (radiusKm != null && (postcode == null || postcode.isBlank())) {
            throw new RadiusWithoutPostcodeException();
        }

        Set<String> categoryFilter = resolveCategoryFilter(categoryId);
        boolean priceFilterActive = priceMinCents != null || priceMaxCents != null;
        String qLower = (q == null) ? null : q.toLowerCase();
        Integer prefixLen = (postcode != null && radiusKm != null) ? prefixLengthFor(radiusKm) : null;

        List<Listing> matches = listingRepository.findAll().stream()
                .filter(listing -> listing.getStatus() == ListingStatus.published)
                .filter(listing -> qLower == null || matchesText(listing, qLower))
                .filter(listing -> categoryFilter == null || categoryFilter.contains(listing.getCategoryId()))
                .filter(listing -> matchesPrice(listing, priceFilterActive, priceMinCents, priceMaxCents))
                .filter(listing -> matchesPostcode(listing, postcode, prefixLen))
                .sorted(Comparator.comparing(Listing::getId))
                .toList();

        int offset = (cursor == null || cursor.isBlank()) ? 0 : CursorCodec.decode(cursor);
        if (offset > matches.size()) {
            offset = matches.size();
        }
        int end = Math.min(offset + limit, matches.size());
        List<Listing> page = matches.subList(offset, end);
        String nextCursor = end < matches.size() ? CursorCodec.encode(end) : null;
        return new ListingPage(page, nextCursor);
    }

    private Set<String> resolveCategoryFilter(String categoryId) {
        if (categoryId == null) {
            return null;
        }
        if (categoryService.isTopLevel(categoryId)) {
            return new LinkedHashSet<>(categoryService.leafIdsUnder(categoryId));
        }
        return Set.of(categoryId);
    }

    private boolean matchesText(Listing listing, String qLower) {
        String title = listing.getTitle() == null ? "" : listing.getTitle().toLowerCase();
        String description = listing.getDescription() == null ? "" : listing.getDescription().toLowerCase();
        return title.contains(qLower) || description.contains(qLower);
    }

    private boolean matchesPrice(Listing listing, boolean priceFilterActive, Long min, Long max) {
        if (!priceFilterActive) {
            return true;
        }
        Long price = listing.getPriceCents();
        if (price == null) {
            // Invariant: a null price cannot satisfy a numeric bound, so it never
            // matches while either priceMinCents or priceMaxCents is supplied.
            return false;
        }
        if (min != null && price < min) {
            return false;
        }
        return max == null || price <= max;
    }

    private boolean matchesPostcode(Listing listing, String postcode, Integer prefixLen) {
        if (postcode == null) {
            return true;
        }
        String listingPostcode = listing.getPostcode();
        if (listingPostcode == null) {
            return false;
        }
        if (prefixLen == null) {
            return listingPostcode.equals(postcode);
        }
        int len = Math.min(prefixLen, Math.min(listingPostcode.length(), postcode.length()));
        return listingPostcode.substring(0, len).equals(postcode.substring(0, len));
    }

    /**
     * No geodata backs this service (postcodes are opaque 5-digit strings, no
     * lat/lng). {@code radiusKm} is approximated by widening the postcode
     * prefix match: tighter radii require more matching leading digits. This
     * is a documented simplification, not a real geo distance.
     */
    private int prefixLengthFor(int radiusKm) {
        if (radiusKm <= 5) {
            return 5;
        }
        if (radiusKm <= 15) {
            return 3;
        }
        if (radiusKm <= 30) {
            return 2;
        }
        return 1;
    }

    // ---- POST /listings -----------------------------------------------------

    public Listing create(ListingCreateRequest request) {
        categoryService.requireLeafCategory(request.getCategoryId());
        sellerService.requireExists(request.getSellerId());

        Listing listing = new Listing();
        listing.setId(listingRepository.nextId());
        listing.setSellerId(request.getSellerId());
        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription() == null ? "" : request.getDescription());
        listing.setCategoryId(request.getCategoryId());
        listing.setPriceCents(request.getPriceCents());
        listing.setCurrency(request.getCurrency() == null || request.getCurrency().isBlank() ? "EUR" : request.getCurrency());
        listing.setStatus(ListingStatus.draft);
        listing.setPostcode(request.getPostcode());
        listing.setCreatedAt(Instant.now());
        listing.setPublishedAt(null);
        listing.setImageIds(new ArrayList<>(request.getImageIds()));

        listingRepository.save(listing);
        return listing;
    }

    // ---- GET /listings/{id} -------------------------------------------------

    public Listing get(String id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No listing with id " + id + "."));
        if (listing.getStatus() == ListingStatus.deleted) {
            throw new ListingDeletedException("Listing " + id + " is deleted.");
        }
        return listing;
    }

    // ---- PATCH /listings/{id} ------------------------------------------------

    public Listing patch(String id, JsonNode patch) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No listing with id " + id + "."));
        if (listing.getStatus() == ListingStatus.deleted) {
            throw new TerminalStatusException("Listing " + id + " is deleted and cannot be modified.");
        }
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

        Listing updated = listing.copy();

        if (patch.has("title")) {
            String title = textValue(patch, "title");
            if (title.length() < 3 || title.length() > 80) {
                throw new ValidationFailedException("title must be between 3 and 80 characters, got " + title.length() + ".");
            }
            updated.setTitle(title);
        }
        if (patch.has("description")) {
            String description = textValue(patch, "description");
            if (description.length() > 4000) {
                throw new ValidationFailedException("description must be at most 4000 characters.");
            }
            updated.setDescription(description);
        }
        if (patch.has("categoryId")) {
            String categoryId = textValue(patch, "categoryId");
            if (!categoryId.matches(Patterns.CATEGORY_ID)) {
                throw new ValidationFailedException("categoryId is malformed.");
            }
            categoryService.requireLeafCategory(categoryId);
            updated.setCategoryId(categoryId);
        }
        if (patch.has("priceCents")) {
            JsonNode node = patch.get("priceCents");
            if (!node.isIntegralNumber()) {
                throw new ValidationFailedException("priceCents must be an integer number of cents, got " + node.asText() + ".");
            }
            long value = node.asLong();
            if (value < 0) {
                throw new ValidationFailedException("priceCents must be an integer number of cents >= 0.");
            }
            updated.setPriceCents(value);
        }
        if (patch.has("currency")) {
            String currency = textValue(patch, "currency");
            if (!currency.matches(Patterns.CURRENCY)) {
                throw new ValidationFailedException("currency is malformed.");
            }
            updated.setCurrency(currency);
        }
        if (patch.has("postcode")) {
            String postcode = textValue(patch, "postcode");
            if (!postcode.matches(Patterns.POSTCODE)) {
                throw new ValidationFailedException("postcode is malformed.");
            }
            updated.setPostcode(postcode);
        }
        if (patch.has("imageIds")) {
            JsonNode arr = patch.get("imageIds");
            if (!arr.isArray()) {
                throw new ValidationFailedException("imageIds must be an array.");
            }
            if (arr.size() > 20) {
                throw new ValidationFailedException("imageIds may have at most 20 entries.");
            }
            List<String> imageIds = new ArrayList<>();
            for (JsonNode item : arr) {
                if (!item.isTextual()) {
                    throw new ValidationFailedException("imageIds entries must be strings.");
                }
                imageIds.add(item.asText());
            }
            updated.setImageIds(imageIds);
        }

        listingRepository.save(updated);
        return updated;
    }

    private String textValue(JsonNode patch, String field) {
        JsonNode value = patch.get(field);
        if (!value.isTextual()) {
            throw new ValidationFailedException(field + " must be a string.");
        }
        return value.asText();
    }

    // ---- POST /listings/{id}/status -------------------------------------

    public Listing changeStatus(String id, StatusChangeTarget target, String reason) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No listing with id " + id + "."));
        ListingStatus from = listing.getStatus();
        ListingStatus to = ListingStatus.valueOf(target.name());
        Set<ListingStatus> allowed = LEGAL_TRANSITIONS.getOrDefault(from, Set.of());

        if (!allowed.contains(to)) {
            String detail;
            if (from == ListingStatus.deleted) {
                detail = "Listing " + id + " is deleted. deleted is terminal.";
            } else if (allowed.isEmpty()) {
                detail = "Listing " + id + " is in status " + from + "; " + from + " cannot move to any other status.";
            } else {
                detail = "Listing " + id + " is in status " + from + "; " + from + " may only move to "
                        + allowed.stream().map(Enum::name).collect(Collectors.joining(" or ")) + ".";
            }
            throw new IllegalStatusTransitionException(detail);
        }

        Listing updated = listing.copy();
        updated.setStatus(to);
        if (to == ListingStatus.published && updated.getPublishedAt() == null) {
            updated.setPublishedAt(Instant.now());
        }
        listingRepository.save(updated);
        statusHistoryRepository.record(new StatusHistoryEntry(id, from, to, reason, Instant.now()));
        return updated;
    }
}
