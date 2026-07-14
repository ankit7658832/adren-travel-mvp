package com.adren.travel.supplier.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.supplier.SupplierCredentialSummary;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.UpdateSupplierCredentialCommand;
import com.adren.travel.supplier.internal.hotelbeds.HotelbedsClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierAggregationServiceTest {

    @Mock
    HotelbedsClient hotelbedsClient;

    @Mock
    SupplierCredentialRepository credentialRepository;

    @Mock
    SupplierCredentialAuditLogRepository auditLogRepository;

    @Mock
    SupplierSecretsService supplierSecretsService;

    SupplierAggregationService service;

    @BeforeEach
    void setUp() {
        service = new SupplierAggregationService(
            hotelbedsClient, credentialRepository, auditLogRepository, supplierSecretsService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateSupplierCredentialCreatesANewRowAndWritesAnAuditLogEntry() {
        authenticateAsSuperAdmin();
        when(credentialRepository.findById(SupplierId.HOTELBEDS)).thenReturn(Optional.empty());
        when(supplierSecretsService.storeSecret(SupplierId.HOTELBEDS, "secret-abc"))
            .thenReturn("arn:aws:secretsmanager:ap-south-1:000000000000:secret:adren/supplier-credentials/HOTELBEDS");

        service.updateSupplierCredential(new UpdateSupplierCredentialCommand(SupplierId.HOTELBEDS, "secret-abc"));

        ArgumentCaptor<SupplierCredential> credentialCaptor = ArgumentCaptor.forClass(SupplierCredential.class);
        verify(credentialRepository).save(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().getSecretArn())
            .isEqualTo("arn:aws:secretsmanager:ap-south-1:000000000000:secret:adren/supplier-credentials/HOTELBEDS");

        ArgumentCaptor<SupplierCredentialAuditLog> captor = ArgumentCaptor.forClass(SupplierCredentialAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }

    @Test
    void updateSupplierCredentialNeverPersistsTheRawSecretValueFND11() {
        authenticateAsSuperAdmin();
        when(credentialRepository.findById(SupplierId.HOTELBEDS)).thenReturn(Optional.empty());
        when(supplierSecretsService.storeSecret(SupplierId.HOTELBEDS, "super-secret-raw-value"))
            .thenReturn("arn:aws:secretsmanager:ap-south-1:000000000000:secret:adren/supplier-credentials/HOTELBEDS");

        service.updateSupplierCredential(new UpdateSupplierCredentialCommand(SupplierId.HOTELBEDS, "super-secret-raw-value"));

        ArgumentCaptor<SupplierCredential> captor = ArgumentCaptor.forClass(SupplierCredential.class);
        verify(credentialRepository).save(captor.capture());
        assertThat(captor.getValue().getSecretArn()).doesNotContain("super-secret-raw-value");
    }

    @Test
    void listSupplierCredentialsNeverExposesTheRawSecretValue() {
        SupplierCredential credential = new SupplierCredential(SupplierId.HOTELBEDS,
            "arn:aws:secretsmanager:ap-south-1:000000000000:secret:adren/supplier-credentials/HOTELBEDS", UUID.randomUUID());
        when(credentialRepository.findAll()).thenReturn(List.of(credential));

        List<SupplierCredentialSummary> summaries = service.listSupplierCredentials();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).supplierId()).isEqualTo(SupplierId.HOTELBEDS);
        assertThat(summaries.get(0).configured()).isTrue();
        // SupplierCredentialSummary has no secretValue/secretArn field at
        // all — the absence itself is the guarantee; this assertion
        // documents intent.
        assertThat(summaries.get(0).toString()).doesNotContain("secretsmanager");
    }

    private static UUID authenticateAsSuperAdmin() {
        UUID userId = UUID.randomUUID();
        AdrenPrincipal principal = new AdrenPrincipal(userId, Role.SUPER_ADMIN, null);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        return userId;
    }
}
