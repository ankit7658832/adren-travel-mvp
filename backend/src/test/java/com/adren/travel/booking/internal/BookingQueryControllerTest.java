package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the RULES.md §3.4 paginated response shape
 * ({@code content/page/size/totalElements/totalPages}) — a plain MockMvc
 * standalone setup (no security filters) is the right tier here since this
 * story is about the collection-response contract, not auth.
 */
class BookingQueryControllerTest {

    private final BookingApi bookingApi = mock(BookingApi.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BookingQueryController(bookingApi))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    @Test
    void returnsThePaginatedResponseShape() throws Exception {
        UUID consultantId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        when(bookingApi.findBookingsByConsultant(eq(consultantId), any()))
            .thenReturn(new PageImpl<>(List.of(bookingId), pageable, 1));

        mockMvc.perform(get("/api/v1/bookings").param("consultantId", consultantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0]").value(bookingId.toString()))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
    }
}
