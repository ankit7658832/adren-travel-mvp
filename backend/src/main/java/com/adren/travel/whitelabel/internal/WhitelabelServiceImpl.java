package com.adren.travel.whitelabel.internal;

import com.adren.travel.security.CapabilityGrantService;
import com.adren.travel.security.CapabilityGrantService.Capability;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.whitelabel.AddUserCommand;
import com.adren.travel.whitelabel.ConsultantUserView;
import com.adren.travel.whitelabel.KycFieldDefinition;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.OnboardConsultantCommand;
import com.adren.travel.whitelabel.WhitelabelApi;
import com.adren.travel.whitelabel.event.ConsultantOnboardedEvent;
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
    private final MarketKycRuleProvider kycRuleProvider;
    private final CapabilityGrantService capabilityGrantService;
    private final ApplicationEventPublisher events;

    WhitelabelServiceImpl(ConsultantRepository consultantRepository, ConsultantUserRepository consultantUserRepository,
                           MarketKycRuleProvider kycRuleProvider, CapabilityGrantService capabilityGrantService,
                           ApplicationEventPublisher events) {
        this.consultantRepository = consultantRepository;
        this.consultantUserRepository = consultantUserRepository;
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
