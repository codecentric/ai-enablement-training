package com.kiezmarkt.listing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Saved search validation tests, focusing on the radius-without-postcode
 * constraint that applies to both POST and PATCH operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SavedSearchTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postSavedSearchWithRadiusButNoPostcodeReturns400() throws Exception {
        String body = """
                {
                  "label": "Test Search",
                  "filters": {
                    "radiusKm": 10
                  }
                }
                """;

        mockMvc.perform(post("/saved-searches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://kiezmarkt.example/problems/radius-without-postcode"));
    }

    @Test
    void postSavedSearchWithRadiusAndPostcodeSucceeds() throws Exception {
        String body = """
                {
                  "label": "Test Search with Radius",
                  "query": "plattenspieler",
                  "filters": {
                    "postcode": "10437",
                    "radiusKm": 10,
                    "categoryId": "elektronik.audio",
                    "priceMaxCents": 30000
                  },
                  "notify": true
                }
                """;

        mockMvc.perform(post("/saved-searches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("Test Search with Radius"))
                .andExpect(jsonPath("$.filters.postcode").value("10437"))
                .andExpect(jsonPath("$.filters.radiusKm").value(10));
    }
}
