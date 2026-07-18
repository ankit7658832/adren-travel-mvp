package com.adren.travel.supplier.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FND-12's core acceptance criteria: BYOS credentials scoped to the calling Consultant's own tenant only. */
@ExtendWith(MockitoExtension.class)
class ByosCredentialServiceTest {

    @Mock
    ByosCredentialRepository repository;

    @Mock
    KmsEnvelopeEncryptionService encryptionService;

    @Mock
    ApplicationEventPublisher events;

    ByosCredentialService service;

    @BeforeEach
    void setUp() {
        service = new ByosCredentialService(repository, encryptionService, events);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveScopesTheNewCredentialToTheCallingConsultantsOwnAccount() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(repository.findByConsultantIdAndSupplierId(consultantId, SupplierId.HOTELBEDS)).thenReturn(Optional.empty());
        var encrypted = new KmsEnvelopeEncryptionService.EncryptedPayload(
            "cipher".getBytes(), "iv".getBytes(), "wrapped".getBytes());
        when(encryptionService.encrypt("raw-secret")).thenReturn(encrypted);

        service.save(SupplierId.HOTELBEDS, "raw-secret");

        ArgumentCaptor<ByosCredential> captor = ArgumentCaptor.forClass(ByosCredential.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getConsultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().getSupplierId()).isEqualTo(SupplierId.HOTELBEDS);
    }

    @Test
    void saveRotatesAnExistingCredentialRow() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        ByosCredential existing = new ByosCredential(UUID.randomUUID(), consultantId, SupplierId.HOTELBEDS,
            "old-cipher".getBytes(), "old-iv".getBytes(), "old-wrapped".getBytes(), UUID.randomUUID());
        when(repository.findByConsultantIdAndSupplierId(consultantId, SupplierId.HOTELBEDS)).thenReturn(Optional.of(existing));
        var encrypted = new KmsEnvelopeEncryptionService.EncryptedPayload(
            "new-cipher".getBytes(), "new-iv".getBytes(), "new-wrapped".getBytes());
        when(encryptionService.encrypt("rotated-secret")).thenReturn(encrypted);

        service.save(SupplierId.HOTELBEDS, "rotated-secret");

        verify(repository).save(existing);
        assertThat(existing.getCiphertext()).isEqualTo("new-cipher".getBytes());
    }

    @Test
    void readReturnsTheDecryptedCredentialForTheCallersOwnConsultant() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        ByosCredential credential = new ByosCredential(UUID.randomUUID(), consultantId, SupplierId.HOTELBEDS,
            "cipher".getBytes(), "iv".getBytes(), "wrapped".getBytes(), UUID.randomUUID());
        when(repository.findByConsultantIdAndSupplierId(consultantId, SupplierId.HOTELBEDS)).thenReturn(Optional.of(credential));
        when(encryptionService.decrypt(any())).thenReturn("decrypted-secret");

        String result = service.read(consultantId, SupplierId.HOTELBEDS);

        assertThat(result).isEqualTo("decrypted-secret");
    }

    @Test
    void consultantBCannotReadConsultantAsByosCredentialFND12() {
        UUID consultantA = UUID.randomUUID();
        UUID consultantB = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantB);

        assertThatThrownBy(() -> service.read(consultantA, SupplierId.HOTELBEDS))
            .isInstanceOf(AccessDeniedException.class);
        verify(repository, org.mockito.Mockito.never()).findByConsultantIdAndSupplierId(any(), any());
    }

    @Test
    void aSuperAdminCanReadAnyConsultantsByosCredentialViaTheExplicitViewAllPath() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.SUPER_ADMIN, null);
        ByosCredential credential = new ByosCredential(UUID.randomUUID(), consultantId, SupplierId.HOTELBEDS,
            "cipher".getBytes(), "iv".getBytes(), "wrapped".getBytes(), UUID.randomUUID());
        when(repository.findByConsultantIdAndSupplierId(consultantId, SupplierId.HOTELBEDS)).thenReturn(Optional.of(credential));
        when(encryptionService.decrypt(any())).thenReturn("decrypted-secret");

        assertThat(service.read(consultantId, SupplierId.HOTELBEDS)).isEqualTo("decrypted-secret");
    }

    @Test
    void readRejectsAnUnknownSupplierForThatConsultant() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(repository.findByConsultantIdAndSupplierId(consultantId, SupplierId.HOTELBEDS)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.read(consultantId, SupplierId.HOTELBEDS))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveWithNoExistingRowPublishesTheSavedEventDMC06() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(repository.findByConsultantIdAndSupplierId(consultantId, SupplierId.HOTELBEDS)).thenReturn(Optional.empty());
        when(encryptionService.encrypt(any())).thenReturn(
            new KmsEnvelopeEncryptionService.EncryptedPayload("c".getBytes(), "i".getBytes(), "w".getBytes()));

        service.save(SupplierId.HOTELBEDS, "raw-secret");

        ArgumentCaptor<com.adren.travel.supplier.event.ByosCredentialSavedEvent> captor =
            ArgumentCaptor.forClass(com.adren.travel.supplier.event.ByosCredentialSavedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().consultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().supplierId()).isEqualTo(SupplierId.HOTELBEDS);
    }

    @Test
    void readForCurrentConsultantReturnsEmptyWhenNoByosCredentialIsConfiguredDMC07() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(repository.findByConsultantIdAndSupplierId(consultantId, SupplierId.HOTELBEDS)).thenReturn(Optional.empty());

        assertThat(service.readForCurrentConsultant(SupplierId.HOTELBEDS)).isEmpty();
    }

    @Test
    void readForCurrentConsultantReturnsTheDecryptedSecretWhenConfiguredDMC07() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        ByosCredential credential = new ByosCredential(UUID.randomUUID(), consultantId, SupplierId.HOTELBEDS,
            "cipher".getBytes(), "iv".getBytes(), "wrapped".getBytes(), UUID.randomUUID());
        when(repository.findByConsultantIdAndSupplierId(consultantId, SupplierId.HOTELBEDS)).thenReturn(Optional.of(credential));
        when(encryptionService.decrypt(any())).thenReturn("consultant-own-secret");

        assertThat(service.readForCurrentConsultant(SupplierId.HOTELBEDS)).contains("consultant-own-secret");
    }

    @Test
    void readForCurrentConsultantIsEmptyForASuperAdminWithNoTenantOfTheirOwn() {
        authenticateAs(Role.SUPER_ADMIN, null);

        assertThat(service.readForCurrentConsultant(SupplierId.HOTELBEDS)).isEmpty();
        verify(repository, org.mockito.Mockito.never()).findByConsultantIdAndSupplierId(any(), any());
    }

    @Test
    void readForCurrentConsultantNeverAcceptsAConsultantIdParameterToSpoofFND03() {
        // Structural proof, not a runtime one: readForCurrentConsultant's
        // only source of tenant scope is CurrentPrincipal — there is no
        // consultantId parameter on the method signature at all, so a
        // cross-tenant read via this path is not just rejected, it's not
        // expressible in the first place (DMC-09).
        assertThat(java.util.Arrays.stream(ByosCredentialService.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("readForCurrentConsultant"))
            .findFirst().orElseThrow().getParameterTypes())
            .containsExactly(SupplierId.class);
    }

    @Test
    void findByosCredentialsForCurrentConsultantListsOnlyTheCallersOwnSuppliersDMC06() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        ByosCredential credential = new ByosCredential(UUID.randomUUID(), consultantId, SupplierId.HOTELBEDS,
            "cipher".getBytes(), "iv".getBytes(), "wrapped".getBytes(), UUID.randomUUID());
        when(repository.findByConsultantId(consultantId)).thenReturn(List.of(credential));

        var result = service.findByosCredentialsForCurrentConsultant();

        assertThat(result).extracting(com.adren.travel.supplier.ByosCredentialSummary::supplierId)
            .containsExactly(SupplierId.HOTELBEDS);
        assertThat(result).extracting(com.adren.travel.supplier.ByosCredentialSummary::configured).containsExactly(true);
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
