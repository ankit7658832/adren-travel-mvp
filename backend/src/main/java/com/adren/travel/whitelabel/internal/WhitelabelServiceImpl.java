package com.adren.travel.whitelabel.internal;

import com.adren.travel.whitelabel.KycFieldDefinition;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.OnboardConsultantCommand;
import com.adren.travel.whitelabel.WhitelabelApi;
import com.adren.travel.whitelabel.event.ConsultantOnboardedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
class WhitelabelServiceImpl implements WhitelabelApi {

    private final ConsultantRepository consultantRepository;
    private final MarketKycRuleProvider kycRuleProvider;
    private final ApplicationEventPublisher events;

    WhitelabelServiceImpl(ConsultantRepository consultantRepository, MarketKycRuleProvider kycRuleProvider,
                           ApplicationEventPublisher events) {
        this.consultantRepository = consultantRepository;
        this.kycRuleProvider = kycRuleProvider;
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
