package com.adren.travel.whitelabel.internal;

import com.adren.travel.shared.TraceIds;
import com.adren.travel.whitelabel.KycFieldDefinition;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConsultantControllerTest {

    private final WhitelabelApi whitelabelApi = mock(WhitelabelApi.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ConsultantController(whitelabelApi))
            .setControllerAdvice(new WhitelabelControllerAdvice())
            .build();
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void rejectsAnOnboardingRequestMissingABusinessName() throws Exception {
        mockMvc.perform(post("/api/v1/consultants")
                .contentType("application/json")
                .content("{\"homeMarket\": \"INDIA\", \"kycFields\": {}}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void aMissingBusinessNameProducesAnRfc7807ProblemDetailWithFieldErrorsAndTraceId() throws Exception {
        MDC.put(TraceIds.MDC_KEY, "trace-consultant-1");

        mockMvc.perform(post("/api/v1/consultants")
                .contentType("application/json")
                .content("{\"homeMarket\": \"INDIA\", \"kycFields\": {}}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.type").value("https://docs.adren.travel/errors/validation-failed"))
            .andExpect(jsonPath("$.traceId").value("trace-consultant-1"))
            .andExpect(jsonPath("$.errors[0].field").value("businessName"));
    }

    @Test
    void onboardsAConsultantAndReturns201WithTheNewId() throws Exception {
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.onboardConsultant(any())).thenReturn(consultantId);

        mockMvc.perform(post("/api/v1/consultants")
                .contentType("application/json")
                .content("{\"businessName\": \"Test Co\", \"homeMarket\": \"INDIA\", "
                    + "\"kycFields\": {\"gstRegistration\": \"GST1\"}}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.consultantId").value(consultantId.toString()));
    }

    @Test
    void returnsTheKycRuleSetForAMarket() throws Exception {
        when(whitelabelApi.requiredKycFieldsFor(any())).thenReturn(List.of(
            new KycFieldDefinition("gstRegistration", "GST Registration", true)));

        mockMvc.perform(get("/api/v1/consultants/kyc-rules").param("market", "INDIA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].fieldKey").value("gstRegistration"))
            .andExpect(jsonPath("$[0].required").value(true));
    }
}
