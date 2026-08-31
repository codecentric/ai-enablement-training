package com.kiezmarkt.listing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Invariant 3 (domain.md): money is always an integer number of cents, and
 * the one place that invariant is declared wrong on purpose — {@code
 * Listing.priceCents} — must still serialize {@code null} verbatim: not
 * {@code 0}, not omitted.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NullPricedListingTests {

    // Seeded listing whose priceCents is null in seed.json (import-feed "Zu verschenken" case).
    private static final String NULL_PRICED_LISTING_ID = "lst_2f7d1e";
    private static final String NULL_PRICED_LISTING_TITLE_FRAGMENT = "Canon EOS 600D";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getListingByIdSerializesNullPriceAsJsonNull() throws Exception {
        mockMvc.perform(get("/listings/{id}", NULL_PRICED_LISTING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NULL_PRICED_LISTING_ID))
                .andExpect(content().string(containsString("\"priceCents\":null")));
    }

    @Test
    void searchResultsSerializeNullPriceAsJsonNullNotZero() throws Exception {
        mockMvc.perform(get("/listings").param("q", NULL_PRICED_LISTING_TITLE_FRAGMENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(NULL_PRICED_LISTING_ID))
                .andExpect(content().string(containsString("\"priceCents\":null")));
    }

    @Test
    void priceCentsFieldIsPresentInTheRenderedListingJson() throws Exception {
        mockMvc.perform(get("/listings/{id}", "lst_d7fd42"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"priceCents\"")));
    }
}
