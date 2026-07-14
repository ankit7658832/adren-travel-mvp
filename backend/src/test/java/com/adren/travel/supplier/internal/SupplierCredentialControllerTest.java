package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierCredentialSummary;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SupplierCredentialControllerTest {

    private final SupplierSearchApi supplierSearchApi = mock(SupplierSearchApi.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SupplierCredentialController(supplierSearchApi)).build();
    }

    @Test
    void updatesACredential() throws Exception {
        mockMvc.perform(put("/api/v1/suppliers/{supplierId}/credentials", "HOTELBEDS")
                .contentType("application/json")
                .content("{\"secretValue\": \"new-secret\"}"))
            .andExpect(status().isOk());

        verify(supplierSearchApi).updateSupplierCredential(any());
    }

    @Test
    void rejectsABlankSecretValue() throws Exception {
        mockMvc.perform(put("/api/v1/suppliers/{supplierId}/credentials", "HOTELBEDS")
                .contentType("application/json")
                .content("{\"secretValue\": \"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listsCredentialSummariesWithoutTheRawSecret() throws Exception {
        when(supplierSearchApi.listSupplierCredentials()).thenReturn(List.of(
            new SupplierCredentialSummary(SupplierId.HOTELBEDS, true, UUID.randomUUID(), Instant.now())));

        mockMvc.perform(get("/api/v1/suppliers/credentials"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].supplierId").value("HOTELBEDS"))
            .andExpect(jsonPath("$[0].configured").value(true));
    }
}
