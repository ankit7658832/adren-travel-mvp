package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DMC-07 — proves the resolver, not {@code HotelbedsClient}, decides which
 * credential source wins: BYOS first, Adren's own as the fallback, and
 * empty when neither is configured. {@code HotelbedsClient} never appears
 * in this test — by design, it has no branching logic of its own to test.
 */
@ExtendWith(MockitoExtension.class)
class SupplierCredentialResolverTest {

    @Mock
    ByosCredentialService byosCredentialService;

    @Mock
    SupplierCredentialRepository credentialRepository;

    @Mock
    SupplierSecretsService supplierSecretsService;

    SupplierCredentialResolver resolver;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        resolver = new SupplierCredentialResolver(byosCredentialService, credentialRepository, supplierSecretsService);
    }

    @Test
    void resolvesTheConsultantsOwnByosCredentialWhenOneIsConfiguredDMC07() {
        when(byosCredentialService.readForCurrentConsultant(SupplierId.HOTELBEDS))
            .thenReturn(Optional.of("consultant-own-hotelbeds-key"));

        Optional<String> resolved = resolver.resolve(SupplierId.HOTELBEDS);

        assertThat(resolved).contains("consultant-own-hotelbeds-key");
        verify(credentialRepository, never()).findById(any());
    }

    @Test
    void fallsBackToAdrensOwnCredentialWhenNoByosCredentialIsConfiguredDMC07() {
        when(byosCredentialService.readForCurrentConsultant(SupplierId.HOTELBEDS)).thenReturn(Optional.empty());
        SupplierCredential adrenCredential = new SupplierCredential(SupplierId.HOTELBEDS,
            "arn:aws:secretsmanager:ap-south-1:000000000000:secret:adren/supplier-credentials/HOTELBEDS", java.util.UUID.randomUUID());
        when(credentialRepository.findById(SupplierId.HOTELBEDS)).thenReturn(Optional.of(adrenCredential));
        when(supplierSecretsService.getSecretValue(adrenCredential.getSecretArn())).thenReturn("adren-own-hotelbeds-key");

        Optional<String> resolved = resolver.resolve(SupplierId.HOTELBEDS);

        assertThat(resolved).contains("adren-own-hotelbeds-key");
    }

    @Test
    void resolvesEmptyWhenNeitherSourceHasACredentialConfigured() {
        when(byosCredentialService.readForCurrentConsultant(SupplierId.HOTELBEDS)).thenReturn(Optional.empty());
        when(credentialRepository.findById(SupplierId.HOTELBEDS)).thenReturn(Optional.empty());

        assertThat(resolver.resolve(SupplierId.HOTELBEDS)).isEmpty();
    }
}
