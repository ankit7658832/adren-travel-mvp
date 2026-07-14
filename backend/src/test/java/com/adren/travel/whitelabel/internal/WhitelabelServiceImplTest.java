package com.adren.travel.whitelabel.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.CapabilityGrantService;
import com.adren.travel.security.CapabilityGrantService.Capability;
import com.adren.travel.security.Role;
import com.adren.travel.whitelabel.AddUserCommand;
import com.adren.travel.whitelabel.ConsultantStatus;
import com.adren.travel.whitelabel.ConsultantUserView;
import com.adren.travel.whitelabel.ConsultantView;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.OnboardConsultantCommand;
import com.adren.travel.whitelabel.event.ConsultantOnboardedEvent;
import com.adren.travel.whitelabel.event.ConsultantStatusChangedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhitelabelServiceImplTest {

    @Mock
    ConsultantRepository consultantRepository;

    @Mock
    ConsultantUserRepository consultantUserRepository;

    @Mock
    CapabilityGrantService capabilityGrantService;

    @Mock
    ApplicationEventPublisher events;

    WhitelabelServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WhitelabelServiceImpl(
            consultantRepository, consultantUserRepository, new MarketKycRuleProvider(), capabilityGrantService, events);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void onboardsAConsultantWithAllRequiredKycFieldsAndPublishesEvent() {
        var command = new OnboardConsultantCommand("Test Travel Co", Market.INDIA, Map.of(
            "gstRegistration", "GST123",
            "businessPan", "PAN123",
            "bankDetails", "Bank ABC"
        ));

        var consultantId = service.onboardConsultant(command);

        assertThat(consultantId).isNotNull();
        verify(consultantRepository).save(any());

        ArgumentCaptor<ConsultantOnboardedEvent> captor = ArgumentCaptor.forClass(ConsultantOnboardedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().consultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().homeMarket()).isEqualTo(Market.INDIA);
    }

    @Test
    void rejectsOnboardingWhenARequiredKycFieldIsMissing() {
        var command = new OnboardConsultantCommand("Test Travel Co", Market.INDIA, Map.of(
            "gstRegistration", "GST123"
            // missing businessPan, bankDetails
        ));

        assertThatThrownBy(() -> service.onboardConsultant(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("businessPan");
    }

    @Test
    void rejectsOnboardingWhenKycFieldsIsNullEntirely() {
        var command = new OnboardConsultantCommand("Test Travel Co", Market.DENMARK, null);

        assertThatThrownBy(() -> service.onboardConsultant(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cvrRegistrationNumber");
    }

    @Test
    void requiredKycFieldsForDelegatesToTheRuleProvider() {
        assertThat(service.requiredKycFieldsFor(Market.UK)).extracting("fieldKey")
            .contains("companiesHouseNumber");
    }

    @Test
    void addUserScopesTheNewUserToTheCallingConsultantNeverAClientSuppliedId() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);

        service.addUser(new AddUserCommand("staff@example.com", "Staff Member"));

        ArgumentCaptor<ConsultantUser> captor = ArgumentCaptor.forClass(ConsultantUser.class);
        verify(consultantUserRepository).save(captor.capture());
        assertThat(captor.getValue().getConsultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().getEmail()).isEqualTo("staff@example.com");
    }

    @Test
    void setUserCapabilityGrantsTheCapabilityWhenTheUserBelongsToTheCallingConsultant() {
        UUID consultantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(consultantUserRepository.findById(userId))
            .thenReturn(Optional.of(new ConsultantUser(userId, consultantId, "a@b.com", "A")));

        service.setUserCapability(userId, Capability.CREATE_PACKAGE, true);

        verify(capabilityGrantService).setGranted(userId, Capability.CREATE_PACKAGE, true);
    }

    @Test
    void fnd09ConsultantCannotGrantCapabilitiesToAnotherConsultantsUser() {
        UUID ownConsultantId = UUID.randomUUID();
        UUID otherConsultantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, ownConsultantId);
        when(consultantUserRepository.findById(userId))
            .thenReturn(Optional.of(new ConsultantUser(userId, otherConsultantId, "a@b.com", "A")));

        assertThatThrownBy(() -> service.setUserCapability(userId, Capability.CREATE_PACKAGE, true))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findUsersByConsultantOnlyEverQueriesTheCallersOwnConsultantId() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        UUID userId = UUID.randomUUID();
        ConsultantUser user = new ConsultantUser(userId, consultantId, "a@b.com", "A");
        Page<ConsultantUser> page = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);
        when(consultantUserRepository.findByConsultantId(consultantId, PageRequest.of(0, 20))).thenReturn(page);
        when(capabilityGrantService.isGranted(userId, Capability.CREATE_PACKAGE)).thenReturn(true);

        Page<ConsultantUserView> result = service.findUsersByConsultant(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).canCreatePackage()).isTrue();
    }

    @Test
    void listConsultantsReturnsAPageOfConsultantViews() {
        Consultant consultant = new Consultant(UUID.randomUUID(), "Test Co", Market.INDIA, Map.of());
        Page<Consultant> page = new PageImpl<>(List.of(consultant), PageRequest.of(0, 20), 1);
        when(consultantRepository.findAll(PageRequest.of(0, 20))).thenReturn(page);

        Page<ConsultantView> result = service.listConsultants(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).businessName()).isEqualTo("Test Co");
        assertThat(result.getContent().get(0).status()).isEqualTo(ConsultantStatus.ACTIVE);
    }

    @Test
    void suspendConsultantTransitionsStatusAndPublishesEvent() {
        UUID consultantId = UUID.randomUUID();
        Consultant consultant = new Consultant(consultantId, "Test Co", Market.INDIA, Map.of());
        when(consultantRepository.findById(consultantId)).thenReturn(Optional.of(consultant));

        service.suspendConsultant(consultantId);

        assertThat(consultant.getStatus()).isEqualTo(ConsultantStatus.SUSPENDED);
        verify(consultantRepository).save(consultant);
        ArgumentCaptor<ConsultantStatusChangedEvent> captor = ArgumentCaptor.forClass(ConsultantStatusChangedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().consultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().newStatus()).isEqualTo(ConsultantStatus.SUSPENDED);
    }

    @Test
    void reinstateConsultantTransitionsStatusAndPublishesEvent() {
        UUID consultantId = UUID.randomUUID();
        Consultant consultant = new Consultant(consultantId, "Test Co", Market.INDIA, Map.of());
        consultant.suspend();
        when(consultantRepository.findById(consultantId)).thenReturn(Optional.of(consultant));

        service.reinstateConsultant(consultantId);

        assertThat(consultant.getStatus()).isEqualTo(ConsultantStatus.ACTIVE);
        ArgumentCaptor<ConsultantStatusChangedEvent> captor = ArgumentCaptor.forClass(ConsultantStatusChangedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().newStatus()).isEqualTo(ConsultantStatus.ACTIVE);
    }

    @Test
    void requireConsultantActiveIsANoOpForAnActiveConsultant() {
        UUID consultantId = UUID.randomUUID();
        Consultant consultant = new Consultant(consultantId, "Test Co", Market.INDIA, Map.of());
        when(consultantRepository.findById(consultantId)).thenReturn(Optional.of(consultant));

        service.requireConsultantActive(consultantId);
    }

    @Test
    void requireConsultantActiveRejectsASuspendedConsultantFND05() {
        UUID consultantId = UUID.randomUUID();
        Consultant consultant = new Consultant(consultantId, "Test Co", Market.INDIA, Map.of());
        consultant.suspend();
        when(consultantRepository.findById(consultantId)).thenReturn(Optional.of(consultant));

        assertThatThrownBy(() -> service.requireConsultantActive(consultantId))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireConsultantActiveRejectsAnUnknownConsultantId() {
        UUID consultantId = UUID.randomUUID();
        when(consultantRepository.findById(consultantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireConsultantActive(consultantId))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static void authenticateAs(Role role, UUID consultantId) {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), role, consultantId);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
