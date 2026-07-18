package com.adren.travel.supplier.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierCredentialSummary;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchResult;
import com.adren.travel.supplier.UpdateSupplierCredentialCommand;
import com.adren.travel.supplier.internal.hotelbeds.HotelbedsClient;
import com.adren.travel.supplier.internal.stuba.StubaClient;
import com.adren.travel.supplier.internal.tbo.TboClient;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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
    StubaClient stubaClient;

    @Mock
    TboClient tboClient;

    @Mock
    SupplierContentCacheRepository contentCacheRepository;

    @Mock
    SupplierCredentialRepository credentialRepository;

    @Mock
    SupplierCredentialAuditLogRepository auditLogRepository;

    @Mock
    SupplierSecretsService supplierSecretsService;

    @Mock
    LocalDmcService localDmcService;

    @Mock
    ByosCredentialService byosCredentialService;

    SupplierAggregationService service;

    @BeforeEach
    void setUp() {
        service = new SupplierAggregationService(
            hotelbedsClient, stubaClient, tboClient, new SupplierCircuitBreakerGateway(), contentCacheRepository,
            credentialRepository, auditLogRepository, supplierSecretsService, localDmcService, byosCredentialService);
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

    @Test
    void searchHotelsMergesResultsFromAllThreeHotelSuppliers() {
        when(hotelbedsClient.search("BOM", checkIn(), checkOut())).thenReturn(List.of(hotelResult(SupplierId.HOTELBEDS)));
        when(stubaClient.search("BOM", checkIn(), checkOut())).thenReturn(List.of(hotelResult(SupplierId.STUBA)));
        when(tboClient.search("BOM", checkIn(), checkOut(), null))
            .thenReturn(new TboClient.TboSearchResponse(List.of(hotelResult(SupplierId.TBO)), "trace-id"));

        List<SupplierSearchResult> results = service.searchHotels("BOM", checkIn(), checkOut());

        assertThat(results).extracting(SupplierSearchResult::supplierId)
            .containsExactlyInAnyOrder(SupplierId.HOTELBEDS, SupplierId.STUBA, SupplierId.TBO);
    }

    @Test
    void searchHotelsIsolatesAFailingSupplierBOK26() {
        when(hotelbedsClient.search("BOM", checkIn(), checkOut())).thenReturn(List.of(hotelResult(SupplierId.HOTELBEDS)));
        when(stubaClient.search("BOM", checkIn(), checkOut())).thenThrow(new RuntimeException("simulated STUBA downtime"));
        when(tboClient.search("BOM", checkIn(), checkOut(), null))
            .thenReturn(new TboClient.TboSearchResponse(List.of(hotelResult(SupplierId.TBO)), "trace-id"));

        List<SupplierSearchResult> results = service.searchHotels("BOM", checkIn(), checkOut());

        // The failing supplier is excluded from this cycle (PRD §10.2.1);
        // the other two suppliers' results are unaffected by STUBA's failure.
        assertThat(results).extracting(SupplierSearchResult::supplierId)
            .containsExactlyInAnyOrder(SupplierId.HOTELBEDS, SupplierId.TBO);
    }

    @Test
    void repeatedSupplierFailuresOpenOnlyThatSuppliersBreakerBOK26() {
        SupplierCircuitBreakerGateway gateway = new SupplierCircuitBreakerGateway();

        for (int i = 0; i < 5; i++) {
            gateway.call(SupplierId.STUBA, () -> {
                throw new RuntimeException("simulated STUBA downtime");
            }, List.of());
        }
        List<String> healthySupplierResult = gateway.call(SupplierId.TBO, () -> List.of("ok"), List.of());

        assertThat(gateway.stateOf(SupplierId.STUBA)).isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
        assertThat(gateway.stateOf(SupplierId.TBO)).isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED);
        assertThat(healthySupplierResult).containsExactly("ok");
    }

    @Test
    void searchHotelsEnrichesRatingFromTheContentCacheBOK27() {
        when(hotelbedsClient.search("BOM", checkIn(), checkOut())).thenReturn(List.of(hotelResult(SupplierId.HOTELBEDS)));
        when(stubaClient.search("BOM", checkIn(), checkOut())).thenReturn(List.of());
        when(tboClient.search("BOM", checkIn(), checkOut(), null))
            .thenReturn(new TboClient.TboSearchResponse(List.of(), "trace-id"));
        SupplierContentCache cached = new SupplierContentCache(SupplierId.HOTELBEDS, "rate-id-HOTELBEDS");
        cached.refresh("Cached Hotel Name", 4.3);
        when(contentCacheRepository.findBySupplierIdAndSupplierContentId(SupplierId.HOTELBEDS, "rate-id-HOTELBEDS"))
            .thenReturn(Optional.of(cached));

        List<SupplierSearchResult> results = service.searchHotels("BOM", checkIn(), checkOut());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).rating()).isEqualTo(4.3);
    }

    @Test
    void saveByosCredentialDelegatesToByosCredentialServiceDMC06() {
        var command = new com.adren.travel.supplier.SaveByosCredentialCommand(SupplierId.HOTELBEDS, "raw-secret");

        service.saveByosCredential(command);

        verify(byosCredentialService).save(SupplierId.HOTELBEDS, "raw-secret");
    }

    @Test
    void findByosCredentialsDelegatesToByosCredentialServiceDMC06() {
        var summary = new com.adren.travel.supplier.ByosCredentialSummary(SupplierId.HOTELBEDS, true, java.time.Instant.now());
        when(byosCredentialService.findByosCredentialsForCurrentConsultant()).thenReturn(List.of(summary));

        assertThat(service.findByosCredentials()).containsExactly(summary);
    }

    private static LocalDate checkIn() {
        return LocalDate.of(2026, 8, 1);
    }

    private static LocalDate checkOut() {
        return LocalDate.of(2026, 8, 5);
    }

    private static SupplierSearchResult hotelResult(SupplierId supplierId) {
        return new SupplierSearchResult(supplierId, "rate-id-" + supplierId, "Test Hotel", "Standard Room",
            new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), null);
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
