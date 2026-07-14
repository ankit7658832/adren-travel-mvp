package com.adren.travel.booking.internal;

import com.adren.travel.booking.AlternateOption;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.shared.CurrencyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItineraryControllerTest {

    private final BookingApi bookingApi = mock(BookingApi.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ItineraryController(bookingApi)).build();
    }

    @Test
    void savesAnItineraryAsAQuotation() throws Exception {
        UUID itineraryId = UUID.randomUUID();
        UUID quotationId = UUID.randomUUID();
        when(bookingApi.saveAsQuotation(itineraryId)).thenReturn(quotationId);

        mockMvc.perform(post("/api/v1/itineraries/{itineraryId}/quotation", itineraryId))
            .andExpect(status().isOk());
    }

    @Test
    void returnsAlternatesForALocation() throws Exception {
        UUID itineraryId = UUID.randomUUID();
        when(bookingApi.findAlternates(eq(itineraryId), eq("Goa"), eq("hotel"), any(), any())).thenReturn(List.of(
            new AlternateOption("HOTELBEDS", "rate-1", "Hotel A", "Deluxe",
                BigDecimal.valueOf(5000), CurrencyCode.INR, 4.2)));

        mockMvc.perform(get("/api/v1/itineraries/{itineraryId}/alternates", itineraryId)
                .param("location", "Goa")
                .param("category", "hotel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].supplierId").value("HOTELBEDS"))
            .andExpect(jsonPath("$[0].supplierRateId").value("rate-1"))
            .andExpect(jsonPath("$[0].netRateCurrency").value("INR"));
    }

    @Test
    void defaultsCheckInAndCheckOutWhenNotProvided() throws Exception {
        UUID itineraryId = UUID.randomUUID();
        when(bookingApi.findAlternates(any(), any(), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/itineraries/{itineraryId}/alternates", itineraryId)
                .param("location", "Goa"))
            .andExpect(status().isOk());

        LocalDate expectedCheckIn = LocalDate.now().plusDays(30);
        verify(bookingApi).findAlternates(itineraryId, "Goa", null, expectedCheckIn, expectedCheckIn.plusDays(3));
    }
}
