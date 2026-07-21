package com.adren.travel.whitelabel.internal;

import com.adren.travel.shared.TraceIds;
import com.adren.travel.whitelabel.BrandingProfileView;
import com.adren.travel.whitelabel.ConsultantStatus;
import com.adren.travel.whitelabel.ConsultantView;
import com.adren.travel.whitelabel.KycFieldDefinition;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.UpdateBrandingCommand;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
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
                .content("{\"homeMarket\": \"INDIA\", \"kycFields\": {}, "
                    + "\"email\": \"owner@testco.example\", \"initialPassword\": \"InitialPassword1!\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void aMissingBusinessNameProducesAnRfc7807ProblemDetailWithFieldErrorsAndTraceId() throws Exception {
        MDC.put(TraceIds.MDC_KEY, "trace-consultant-1");

        mockMvc.perform(post("/api/v1/consultants")
                .contentType("application/json")
                .content("{\"homeMarket\": \"INDIA\", \"kycFields\": {}, "
                    + "\"email\": \"owner@testco.example\", \"initialPassword\": \"InitialPassword1!\"}"))
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
                    + "\"kycFields\": {\"gstRegistration\": \"GST1\"}, "
                    + "\"email\": \"owner@testco.example\", \"initialPassword\": \"InitialPassword1!\"}"))
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

    @Test
    void listsConsultantsInThePaginatedShape() throws Exception {
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.listConsultants(any())).thenReturn(new PageImpl<>(List.of(
            new ConsultantView(consultantId, "Test Co", Market.INDIA, ConsultantStatus.ACTIVE, Instant.now())),
            PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/consultants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].businessName").value("Test Co"))
            .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void suspendingAConsultantCallsTheApiWithTheSuspendedStatus() throws Exception {
        UUID consultantId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/consultants/{consultantId}/status", consultantId)
                .contentType("application/json")
                .content("{\"status\": \"SUSPENDED\"}"))
            .andExpect(status().isOk());

        verify(whitelabelApi).suspendConsultant(consultantId);
    }

    @Test
    void reinstatingAConsultantCallsTheApiWithTheActiveStatus() throws Exception {
        UUID consultantId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/consultants/{consultantId}/status", consultantId)
                .contentType("application/json")
                .content("{\"status\": \"ACTIVE\"}"))
            .andExpect(status().isOk());

        verify(whitelabelApi).reinstateConsultant(consultantId);
    }

    @Test
    void rejectsAStatusUpdateWithNoStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/consultants/{consultantId}/status", UUID.randomUUID())
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updatesBrandingWithTheGivenConsultantIdAndFields() throws Exception {
        UUID consultantId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/consultants/{consultantId}/branding", consultantId)
                .contentType("application/json")
                .content("{\"backgroundColor\": \"#FFFFFF\", \"textColorPrimary\": \"#000000\", "
                    + "\"textColorSecondary\": \"#111111\", \"domain\": \"consultant.example.com\"}"))
            .andExpect(status().isOk());

        verify(whitelabelApi).updateBranding(new UpdateBrandingCommand(
            consultantId, null, null, "#FFFFFF", "#000000", "#111111", "consultant.example.com"));
    }

    @Test
    void rejectsABrandingUpdateMissingARequiredTextColor() throws Exception {
        mockMvc.perform(patch("/api/v1/consultants/{consultantId}/branding", UUID.randomUUID())
                .contentType("application/json")
                .content("{\"backgroundColor\": \"#FFFFFF\", \"textColorSecondary\": \"#111111\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void returnsTheCurrentBrandingProfile() throws Exception {
        UUID consultantId = UUID.randomUUID();
        when(whitelabelApi.findBranding(consultantId)).thenReturn(new BrandingProfileView(
            consultantId, "https://cdn/logo.png", null, "#FFFFFF", "#000000", "#111111",
            "consultant.example.com", Instant.now()));

        mockMvc.perform(get("/api/v1/consultants/{consultantId}/branding", consultantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.domain").value("consultant.example.com"))
            .andExpect(jsonPath("$.backgroundColor").value("#FFFFFF"));
    }
}
