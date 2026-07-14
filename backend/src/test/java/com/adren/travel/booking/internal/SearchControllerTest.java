package com.adren.travel.booking.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchControllerTest {

    private final GeocodeAndSearchService geocodeAndSearchService = mock(GeocodeAndSearchService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SearchController(geocodeAndSearchService)).build();
    }

    @Test
    void rejectsAnEmptyLocationListWithA400() throws Exception {
        mockMvc.perform(post("/api/v1/search")
                .contentType("application/json")
                .content("{\"locationQueries\": []}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void returnsAGeocodedLocationPerQuery() throws Exception {
        when(geocodeAndSearchService.geocodeAndSearch(any(), any(), any())).thenReturn(List.of(
            new GeocodedLocation("Goa", "Goa", 15.5, 73.8, true, "rate-1"),
            new GeocodedLocation("Antarctica", "Antarctica", 20.0, 80.0, false, null)
        ));

        mockMvc.perform(post("/api/v1/search")
                .contentType("application/json")
                .content("{\"locationQueries\": [\"Goa\", \"Antarctica\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locations[0].locationCode").value("Goa"))
            .andExpect(jsonPath("$.locations[0].hasInventory").value(true))
            .andExpect(jsonPath("$.locations[1].locationCode").value("Antarctica"))
            .andExpect(jsonPath("$.locations[1].hasInventory").value(false));
    }
}
