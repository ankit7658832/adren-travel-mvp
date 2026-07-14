package com.adren.travel.whitelabel.internal;

import com.adren.travel.security.CapabilityGrantService;
import com.adren.travel.security.CapabilityGrantService.Capability;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.whitelabel.AddUserCommand;
import com.adren.travel.whitelabel.BrandingProfileView;
import com.adren.travel.whitelabel.ConsultantStatus;
import com.adren.travel.whitelabel.ConsultantUserView;
import com.adren.travel.whitelabel.ConsultantView;
import com.adren.travel.whitelabel.KycFieldDefinition;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.OnboardConsultantCommand;
import com.adren.travel.whitelabel.UpdateBrandingCommand;
import com.adren.travel.whitelabel.WhitelabelApi;
import com.adren.travel.whitelabel.event.BrandingUpdatedEvent;
import com.adren.travel.whitelabel.event.ConsultantOnboardedEvent;
import com.adren.travel.whitelabel.event.ConsultantStatusChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
class WhitelabelServiceImpl implements WhitelabelApi {

    private final ConsultantRepository consultantRepository;
    private final ConsultantUserRepository consultantUserRepository;
    private final BrandingProfileRepository brandingProfileRepository;
    private final MarketKycRuleProvider kycRuleProvider;
    private final CapabilityGrantService capabilityGrantService;
    private final ApplicationEventPublisher events;

    WhitelabelServiceImpl(ConsultantRepository consultantRepository, ConsultantUserRepository consultantUserRepository,
                           BrandingProfileRepository brandingProfileRepository, MarketKycRuleProvider kycRuleProvider,
                           CapabilityGrantService capabilityGrantService, ApplicationEventPublisher events) {
        this.consultantRepository = consultantRepository;
        this.consultantUserRepository = consultantUserRepository;
        this.brandingProfileRepository = brandingProfileRepository;
        this.kycRuleProvider = kycRuleProvider;
        this.capabilityGrantService = capabilityGrantService;
        this.events = events;
    }

    @Override
    @Transactional
    public UUID onboardConsultant(OnboardConsultantCommand command) {
        Map<String, String> kycFields = command.kycFields() != null ? command.kycFields() : Map.of();
        List<KycFieldDefinition> requiredFields = kycRuleProvider.requiredFieldsFor(command.homeMarket());
        for (KycFieldDefinition field : requiredFields) {
            if (field.required() && isBlank(kycFields.get(field.fieldKey()))) {
                throw new IllegalArgumentException(
                    "Missing required KYC field for " + command.homeMarket() + ": " + field.fieldKey());
            }
        }

        UUID consultantId = UUID.randomUUID();
        Consultant consultant = new Consultant(consultantId, command.businessName(), command.homeMarket(), kycFields);
        consultantRepository.save(consultant);

        events.publishEvent(new ConsultantOnboardedEvent(consultantId, command.homeMarket()));
        return consultantId;
    }

    @Override
    public List<KycFieldDefinition> requiredKycFieldsFor(Market market) {
        return kycRuleProvider.requiredFieldsFor(market);
    }

    @Override
    @Transactional
    public UUID addUser(AddUserCommand command) {
        UUID consultantId = CurrentPrincipal.get().consultantId();
        UUID userId = UUID.randomUUID();
        ConsultantUser user = new ConsultantUser(userId, consultantId, command.email(), command.displayName());
        consultantUserRepository.save(user);
        return userId;
    }

    @Override
    @Transactional
    public void setUserCapability(UUID userId, Capability capability, boolean granted) {
        ConsultantUser user = consultantUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("No such user: " + userId));

        // RULES.md §5.2 — a Consultant may only grant capabilities to their
        // own Users, never another Consultant's.
        UUID callerConsultantId = CurrentPrincipal.get().consultantId();
        if (!user.getConsultantId().equals(callerConsultantId)) {
            throw new AccessDeniedException("User " + userId + " does not belong to the calling Consultant");
        }

        capabilityGrantService.setGranted(userId, capability, granted);
    }

    @Override
    public Page<ConsultantUserView> findUsersByConsultant(Pageable pageable) {
        UUID consultantId = CurrentPrincipal.get().consultantId();
        return consultantUserRepository.findByConsultantId(consultantId, pageable)
            .map(user -> new ConsultantUserView(
                user.getUserId(), user.getEmail(), user.getDisplayName(),
                capabilityGrantService.isGranted(user.getUserId(), Capability.CREATE_PACKAGE)));
    }

    @Override
    public Page<ConsultantView> listConsultants(Pageable pageable) {
        return consultantRepository.findAll(pageable).map(WhitelabelServiceImpl::toView);
    }

    @Override
    @Transactional
    public void suspendConsultant(UUID consultantId) {
        Consultant consultant = findConsultantOrThrow(consultantId);
        consultant.suspend();
        consultantRepository.save(consultant);
        events.publishEvent(new ConsultantStatusChangedEvent(consultantId, ConsultantStatus.SUSPENDED));
    }

    @Override
    @Transactional
    public void reinstateConsultant(UUID consultantId) {
        Consultant consultant = findConsultantOrThrow(consultantId);
        consultant.reinstate();
        consultantRepository.save(consultant);
        events.publishEvent(new ConsultantStatusChangedEvent(consultantId, ConsultantStatus.ACTIVE));
    }

    @Override
    public void requireConsultantActive(UUID consultantId) {
        Consultant consultant = findConsultantOrThrow(consultantId);
        if (consultant.getStatus() != ConsultantStatus.ACTIVE) {
            throw new AccessDeniedException("Consultant " + consultantId + " is " + consultant.getStatus()
                + " and cannot search or book until reinstated");
        }
    }

    @Override
    @Transactional
    public void updateBranding(UpdateBrandingCommand command) {
        findConsultantOrThrow(command.consultantId());
        BrandingProfile profile = brandingProfileRepository.findById(command.consultantId())
            .map(existing -> {
                existing.update(command.logoUrl(), command.backgroundImageUrl(), command.backgroundColor(),
                    command.textColorPrimary(), command.textColorSecondary(), command.domain());
                return existing;
            })
            .orElseGet(() -> new BrandingProfile(command.consultantId(), command.logoUrl(), command.backgroundImageUrl(),
                command.backgroundColor(), command.textColorPrimary(), command.textColorSecondary(), command.domain()));
        brandingProfileRepository.save(profile);

        events.publishEvent(new BrandingUpdatedEvent(command.consultantId(), command.domain()));
    }

    @Override
    public BrandingProfileView findBranding(UUID consultantId) {
        return brandingProfileRepository.findById(consultantId)
            .map(WhitelabelServiceImpl::toBrandingView)
            .orElseThrow(() -> new IllegalArgumentException("No branding profile for consultant: " + consultantId));
    }

    private static BrandingProfileView toBrandingView(BrandingProfile profile) {
        return new BrandingProfileView(profile.getConsultantId(), profile.getLogoUrl(), profile.getBackgroundImageUrl(),
            profile.getBackgroundColor(), profile.getTextColorPrimary(), profile.getTextColorSecondary(),
            profile.getDomain(), profile.getUpdatedAt());
    }

    private Consultant findConsultantOrThrow(UUID consultantId) {
        return consultantRepository.findById(consultantId)
            .orElseThrow(() -> new IllegalArgumentException("No such consultant: " + consultantId));
    }

    private static ConsultantView toView(Consultant consultant) {
        return new ConsultantView(consultant.getConsultantId(), consultant.getBusinessName(),
            consultant.getHomeMarket(), consultant.getStatus(), consultant.getCreatedAt());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
