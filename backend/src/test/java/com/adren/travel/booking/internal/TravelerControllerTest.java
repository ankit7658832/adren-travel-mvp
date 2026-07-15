package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TravelerControllerTest {

    private final BookingApi bookingApi = mock(BookingApi.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TravelerController(bookingApi))
            .setControllerAdvice(new BookingControllerAdvice())
            .build();
    }

    @Test
    void createsATravelerProfileAndReturns201WithTheNewId() throws Exception {
        UUID travelerId = UUID.randomUUID();
        when(bookingApi.createTravelerProfile(any())).thenReturn(travelerId);

        mockMvc.perform(post("/api/v1/travelers")
                .contentType("application/json")
                .content("{\"name\": \"Jane Traveler\", \"dateOfBirth\": \"1990-05-01\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.travelerId").value(travelerId.toString()));
    }

    @Test
    void rejectsARequestMissingTheRequiredName() throws Exception {
        mockMvc.perform(post("/api/v1/travelers")
                .contentType("application/json")
                .content("{\"dateOfBirth\": \"1990-05-01\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsARequestMissingTheRequiredDateOfBirth() throws Exception {
        mockMvc.perform(post("/api/v1/travelers")
                .contentType("application/json")
                .content("{\"name\": \"Jane Traveler\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void allowsOmittingOptionalPassportFields() throws Exception {
        UUID travelerId = UUID.randomUUID();
        when(bookingApi.createTravelerProfile(any())).thenReturn(travelerId);

        mockMvc.perform(post("/api/v1/travelers")
                .contentType("application/json")
                .content("{\"name\": \"Jane Traveler\", \"dateOfBirth\": \"1990-05-01\"}"))
            .andExpect(status().isCreated());
    }
}
