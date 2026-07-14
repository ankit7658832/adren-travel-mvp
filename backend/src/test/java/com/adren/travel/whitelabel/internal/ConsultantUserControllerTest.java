package com.adren.travel.whitelabel.internal;

import com.adren.travel.security.CapabilityGrantService.Capability;
import com.adren.travel.whitelabel.ConsultantUserView;
import com.adren.travel.whitelabel.WhitelabelApi;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConsultantUserControllerTest {

    private final WhitelabelApi whitelabelApi = mock(WhitelabelApi.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ConsultantUserController(whitelabelApi))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    @Test
    void addsAUserAndReturns201() throws Exception {
        UUID userId = UUID.randomUUID();
        when(whitelabelApi.addUser(any())).thenReturn(userId);

        mockMvc.perform(post("/api/v1/users")
                .contentType("application/json")
                .content("{\"email\": \"staff@example.com\", \"displayName\": \"Staff\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void rejectsAnInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType("application/json")
                .content("{\"email\": \"not-an-email\", \"displayName\": \"Staff\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void setsACapabilityGrant() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/users/{userId}/capabilities/{capability}", userId, "CREATE_PACKAGE")
                .contentType("application/json")
                .content("{\"granted\": true}"))
            .andExpect(status().isOk());

        verify(whitelabelApi).setUserCapability(eq(userId), eq(Capability.CREATE_PACKAGE), eq(true));
    }

    @Test
    void listsUsersInThePaginatedShape() throws Exception {
        UUID userId = UUID.randomUUID();
        when(whitelabelApi.findUsersByConsultant(any())).thenReturn(
            new PageImpl<>(List.of(new ConsultantUserView(userId, "a@b.com", "A", false)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].email").value("a@b.com"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }
}
