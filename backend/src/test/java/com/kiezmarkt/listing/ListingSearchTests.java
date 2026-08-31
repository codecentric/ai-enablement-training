package com.kiezmarkt.listing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /listings: search only ever returns published listings, and category
 * filtering narrows the result set.
 */
@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class ListingSearchTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void searchReturnsOnlyPublishedListings() throws Exception {
        Set<String> statuses = new HashSet<>();
        String cursor = null;
        int pages = 0;
        do {
            String url = "/listings?limit=100" + (cursor != null ? "&cursor=" + cursor : "");
            MvcResult result = mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            for (JsonNode item : body.get("items")) {
                statuses.add(item.get("status").asText());
            }
            JsonNode next = body.get("nextCursor");
            cursor = next.isNull() ? null : next.asText();
            pages++;
        } while (cursor != null && pages < 10);

        assertThat(statuses).containsExactly("published");
    }

    @Test
    void filteringByLeafCategoryNarrowsTheResultSet() throws Exception {
        MvcResult unfiltered = mockMvc.perform(get("/listings?limit=100"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult filtered = mockMvc.perform(get("/listings?categoryId=haushalt.moebel&limit=100"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode unfilteredBody = objectMapper.readTree(unfiltered.getResponse().getContentAsString());
        JsonNode filteredBody = objectMapper.readTree(filtered.getResponse().getContentAsString());

        int unfilteredCount = unfilteredBody.get("items").size();
        int filteredCount = filteredBody.get("items").size();

        assertThat(filteredCount).isLessThan(unfilteredCount);
        for (JsonNode item : filteredBody.get("items")) {
            assertThat(item.get("categoryId").asText()).isEqualTo("haushalt.moebel");
        }
    }

    @Test
    void radiusWithoutPostcodeIsRejected() throws Exception {
        mockMvc.perform(get("/listings?radiusKm=5"))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.type")
                        .value("https://kiezmarkt.example/problems/radius-without-postcode"));
    }

    @Test
    void malformedCursorIsRejected() throws Exception {
        mockMvc.perform(get("/listings?cursor=not-a-valid-cursor!!"))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.type")
                        .value("https://kiezmarkt.example/problems/invalid-cursor"));
    }
}
