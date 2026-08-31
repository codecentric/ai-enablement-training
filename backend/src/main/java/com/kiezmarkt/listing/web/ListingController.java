package com.kiezmarkt.listing.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiezmarkt.listing.domain.Listing;
import com.kiezmarkt.listing.dto.ListingCreateRequest;
import com.kiezmarkt.listing.dto.ListingPage;
import com.kiezmarkt.listing.dto.Patterns;
import com.kiezmarkt.listing.dto.StatusChangeRequest;
import com.kiezmarkt.listing.service.ListingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/listings")
@Validated
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public ListingPage search(
            @RequestParam(required = false) @Size(min = 1, max = 200) String q,
            @RequestParam(required = false) @Pattern(regexp = Patterns.CATEGORY_ID) String categoryId,
            @RequestParam(required = false) @Min(0) Long priceMinCents,
            @RequestParam(required = false) @Min(0) Long priceMaxCents,
            @RequestParam(required = false) @Pattern(regexp = Patterns.POSTCODE) String postcode,
            @RequestParam(required = false) @Min(1) Integer radiusKm,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer limit) {
        return listingService.search(q, categoryId, priceMinCents, priceMaxCents, postcode, radiusKm, cursor, limit);
    }

    @PostMapping
    public ResponseEntity<Listing> create(@Valid @RequestBody ListingCreateRequest request) {
        Listing created = listingService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public Listing get(@PathVariable String id) {
        return listingService.get(id);
    }

    @PatchMapping("/{id}")
    public Listing patch(@PathVariable String id, @RequestBody JsonNode patch) {
        return listingService.patch(id, patch);
    }

    @PostMapping("/{id}/status")
    public Listing changeStatus(@PathVariable String id, @Valid @RequestBody StatusChangeRequest request) {
        return listingService.changeStatus(id, request.getStatus(), request.getReason());
    }
}
