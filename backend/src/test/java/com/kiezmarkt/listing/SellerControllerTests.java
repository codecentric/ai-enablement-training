package com.kiezmarkt.listing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /sellers/{id}: only the SellerPublic projection, ever
 * (domain.md invariant 6 — seller email never leaves the service).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SellerControllerTests {

    private static final String KNOWN_SELLER_ID = "sel_0001";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getSellerNeverExposesEmail() throws Exception {
        mockMvc.perform(get("/sellers/{id}", KNOWN_SELLER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(KNOWN_SELLER_ID))
                .andExpect(jsonPath("$.displayName").exists())
                .andExpect(jsonPath("$.isCommercial").exists())
                .andExpect(content().string(not(containsString("email"))))
                .andExpect(content().string(not(containsString("@"))));
    }

    @Test
    void getSellerReturns404ForAMissingSeller() throws Exception {
        mockMvc.perform(get("/sellers/{id}", "sel_does_not_exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://kiezmarkt.example/problems/not-found"));
    }
}
