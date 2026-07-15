package com.adren.travel.payments.internal;

import com.adren.travel.payments.MarkupRuleView;
import com.adren.travel.payments.MarkupType;
import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.shared.ProductCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarkupRuleControllerTest {

    private final PaymentsApi paymentsApi = mock(PaymentsApi.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MarkupRuleController(paymentsApi))
            .setControllerAdvice(new PaymentsControllerAdvice())
            .build();
    }

    @Test
    void configuresAPercentageMarkupRule() throws Exception {
        UUID consultantId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/consultants/{consultantId}/markup-rules", consultantId)
                .contentType("application/json")
                .content("{\"category\": \"HOTEL\", \"markupType\": \"PERCENTAGE\", \"percentageValue\": 15}"))
            .andExpect(status().isOk());

        verify(paymentsApi).configureMarkup(eq(consultantId), any());
    }

    @Test
    void rejectsARequestMissingTheCategory() throws Exception {
        mockMvc.perform(put("/api/v1/consultants/{consultantId}/markup-rules", UUID.randomUUID())
                .contentType("application/json")
                .content("{\"markupType\": \"PERCENTAGE\", \"percentageValue\": 15}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listsMarkupRulesForAConsultant() throws Exception {
        UUID consultantId = UUID.randomUUID();
        when(paymentsApi.findMarkupRules(consultantId)).thenReturn(List.of(
            new MarkupRuleView(consultantId, ProductCategory.HOTEL, MarkupType.PERCENTAGE,
                BigDecimal.valueOf(15), null, null, Instant.now())));

        mockMvc.perform(get("/api/v1/consultants/{consultantId}/markup-rules", consultantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].category").value("HOTEL"))
            .andExpect(jsonPath("$[0].markupType").value("PERCENTAGE"));
    }
}
