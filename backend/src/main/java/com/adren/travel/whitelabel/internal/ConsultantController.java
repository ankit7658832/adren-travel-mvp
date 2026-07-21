package com.adren.travel.whitelabel.internal;

import com.adren.travel.shared.PageResponse;
import com.adren.travel.whitelabel.BrandingProfileView;
import com.adren.travel.whitelabel.ConsultantView;
import com.adren.travel.whitelabel.KycFieldDefinition;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.OnboardConsultantCommand;
import com.adren.travel.whitelabel.UpdateBrandingCommand;
import com.adren.travel.whitelabel.WhitelabelApi;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PRD §13.1/§21.6 — Super Admin's Consultant onboarding wizard. Controller
 * depends on {@link WhitelabelApi} only (RULES.md §1.2).
 */
@RestController
@RequestMapping("/api/v1/consultants")
class ConsultantController {

    private final WhitelabelApi whitelabelApi;

    ConsultantController(WhitelabelApi whitelabelApi) {
        this.whitelabelApi = whitelabelApi;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> onboard(@Valid @RequestBody OnboardConsultantRequest request) {
        UUID consultantId = whitelabelApi.onboardConsultant(new OnboardConsultantCommand(
            request.businessName(), request.homeMarket(), request.kycFields(), request.email(), request.initialPassword()));
        return Map.of("consultantId", consultantId);
    }

    @GetMapping("/kyc-rules")
    List<KycFieldDefinition> kycRules(@RequestParam Market market) {
        return whitelabelApi.requiredKycFieldsFor(market);
    }

    @GetMapping
    PageResponse<ConsultantView> listConsultants(Pageable pageable) {
        return PageResponse.of(whitelabelApi.listConsultants(pageable));
    }

    @PatchMapping("/{consultantId}/status")
    void updateStatus(@PathVariable UUID consultantId, @Valid @RequestBody UpdateConsultantStatusRequest request) {
        switch (request.status()) {
            case SUSPENDED -> whitelabelApi.suspendConsultant(consultantId);
            case ACTIVE -> whitelabelApi.reinstateConsultant(consultantId);
        }
    }

    @PatchMapping("/{consultantId}/branding")
    void updateBranding(@PathVariable UUID consultantId, @Valid @RequestBody UpdateBrandingRequest request) {
        whitelabelApi.updateBranding(new UpdateBrandingCommand(consultantId, request.logoUrl(),
            request.backgroundImageUrl(), request.backgroundColor(), request.textColorPrimary(),
            request.textColorSecondary(), request.domain()));
    }

    @GetMapping("/{consultantId}/branding")
    BrandingProfileView getBranding(@PathVariable UUID consultantId) {
        return whitelabelApi.findBranding(consultantId);
    }
}
