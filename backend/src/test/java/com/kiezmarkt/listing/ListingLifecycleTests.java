package com.kiezmarkt.listing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The listing lifecycle: read by id in every non-deleted status, create as a
 * draft, publish, and the legal-transition guard on
 * {@code POST /listings/{id}/status}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ListingLifecycleTests {

    // Seeded, stable ids from src/main/resources/seed.json.
    private static final String SEEDED_PUBLISHED_LISTING_ID = "lst_d7fd42";
    private static final String SEEDED_DELETED_LISTING_ID = "lst_7ca25e";
    private static final String KNOWN_SELLER_ID = "sel_0001";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getListingReturns200ForASeededPublishedListing() throws Exception {
        mockMvc.perform(get("/listings/{id}", SEEDED_PUBLISHED_LISTING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEEDED_PUBLISHED_LISTING_ID))
                .andExpect(jsonPath("$.status").value("published"));
    }

    @Test
    void getListingReturns404ForAMissingId() throws Exception {
        mockMvc.perform(get("/listings/{id}", "lst_does_not_exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://kiezmarkt.example/problems/not-found"));
    }

    @Test
    void getListingReturns410ForASeededDeletedListing() throws Exception {
        mockMvc.perform(get("/listings/{id}", SEEDED_DELETED_LISTING_ID))
                .andExpect(status().isGone())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://kiezmarkt.example/problems/listing-deleted"));
    }

    @Test
    void createdListingIsADraftAbsentFromSearchUntilPublished() throws Exception {
        String uniqueTitle = "Lifecycle Test Listing " + System.nanoTime();
        String body = """
                {
                  "sellerId": "%s",
                  "title": "%s",
                  "categoryId": "elektronik.computer",
                  "priceCents": 5000,
                  "postcode": "10119"
                }
                """.formatted(KNOWN_SELLER_ID, uniqueTitle);

        MvcResult createResult = mockMvc.perform(post("/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("draft"))
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode created = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResult.getResponse().getContentAsString());
        String id = created.get("id").asText();
        assertThat(created.get("publishedAt").isNull()).isTrue();

        // Draft must not appear in search results.
        mockMvc.perform(get("/listings").param("q", uniqueTitle))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        // Publish it.
        mockMvc.perform(post("/listings/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"published\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("published"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        // Now it must appear in search results.
        mockMvc.perform(get("/listings").param("q", uniqueTitle))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id));
    }

    @Test
    void illegalTransitionDraftToPausedIsRejected() throws Exception {
        String id = createDraft("Illegal Transition Test " + System.nanoTime());

        mockMvc.perform(post("/listings/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"paused\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://kiezmarkt.example/problems/illegal-status-transition"));
    }

    @Test
    void transitioningToTheCurrentStatusIsRejected() throws Exception {
        String id = createDraft("Same Status Transition Test " + System.nanoTime());
        mockMvc.perform(post("/listings/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"published\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/listings/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"published\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://kiezmarkt.example/problems/illegal-status-transition"));
    }

    @Test
    void nothingTransitionsOutOfDeleted() throws Exception {
        String id = createDraft("Deleted Terminal Test " + System.nanoTime());
        mockMvc.perform(post("/listings/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"deleted\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/listings/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"published\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://kiezmarkt.example/problems/illegal-status-transition"));

        // A deleted listing can no longer be patched either.
        mockMvc.perform(patch("/listings/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"anything at all\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://kiezmarkt.example/problems/terminal-status"));
    }

    private String createDraft(String title) throws Exception {
        String body = """
                {
                  "sellerId": "%s",
                  "title": "%s",
                  "categoryId": "elektronik.computer",
                  "priceCents": 1000,
                  "postcode": "10119"
                }
                """.formatted(KNOWN_SELLER_ID, title);
        MvcResult result = mockMvc.perform(post("/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }
}
