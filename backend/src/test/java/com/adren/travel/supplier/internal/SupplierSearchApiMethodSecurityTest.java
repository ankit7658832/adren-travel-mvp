package com.adren.travel.supplier.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.supplier.UpdateSupplierCredentialCommand;
import com.adren.travel.supplier.internal.hotelbeds.HotelbedsClient;
import com.adren.travel.supplier.internal.stuba.StubaClient;
import com.adren.travel.supplier.internal.tbo.TboClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FND-10's acceptance criterion — only SUPER_ADMIN can manage supplier
 * credentials (PRD §6) — proven against the real {@code @PreAuthorize}-proxied
 * {@link SupplierSearchApi} bean, mirroring
 * {@code BookingApiMethodSecurityTest}'s DB-free approach.
 */
class SupplierSearchApiMethodSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }

        @Bean
        HotelbedsClient hotelbedsClient(WebClient.Builder builder) {
            return new HotelbedsClient(builder);
        }

        @Bean
        StubaClient stubaClient(WebClient.Builder builder) {
            return new StubaClient(builder);
        }

        @Bean
        TboClient tboClient(WebClient.Builder builder) {
            return new TboClient(builder);
        }

        @Bean
        SupplierCircuitBreakerGateway supplierCircuitBreakerGateway() {
            return new SupplierCircuitBreakerGateway();
        }

        @Bean
        SupplierContentCacheRepository supplierContentCacheRepository() {
            return Mockito.mock(SupplierContentCacheRepository.class);
        }

        @Bean
        SupplierCredentialRepository supplierCredentialRepository() {
            return Mockito.mock(SupplierCredentialRepository.class);
        }

        @Bean
        SupplierCredentialAuditLogRepository supplierCredentialAuditLogRepository() {
            return Mockito.mock(SupplierCredentialAuditLogRepository.class);
        }

        @Bean
        SupplierSecretsService supplierSecretsService() {
            return Mockito.mock(SupplierSecretsService.class);
        }

        @Bean
        LocalDmcService localDmcService() {
            return Mockito.mock(LocalDmcService.class);
        }

        @Bean
        SupplierSearchApi supplierSearchApi(HotelbedsClient hotelbedsClient, StubaClient stubaClient,
                                             TboClient tboClient, SupplierCircuitBreakerGateway circuitBreakerGateway,
                                             SupplierContentCacheRepository contentCacheRepository,
                                             SupplierCredentialRepository repo,
                                             SupplierCredentialAuditLogRepository auditRepo,
                                             SupplierSecretsService supplierSecretsService,
                                             LocalDmcService localDmcService) {
            return new SupplierAggregationService(hotelbedsClient, stubaClient, tboClient, circuitBreakerGateway,
                contentCacheRepository, repo, auditRepo, supplierSecretsService, localDmcService);
        }
    }

    private static AnnotationConfigApplicationContext context;
    private static SupplierSearchApi supplierSearchApi;

    @BeforeAll
    static void startContext() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        supplierSearchApi = context.getBean(SupplierSearchApi.class);
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aConsultantCannotUpdateASupplierCredential() {
        authenticateAs(Role.CONSULTANT);

        assertThatThrownBy(() -> supplierSearchApi.updateSupplierCredential(
            new UpdateSupplierCredentialCommand(SupplierId.HOTELBEDS, "secret")))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aConsultantCannotListSupplierCredentials() {
        authenticateAs(Role.CONSULTANT);

        assertThatThrownBy(() -> supplierSearchApi.listSupplierCredentials())
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aConsultantCannotRecordALocalDmcBookingDMC04() {
        authenticateAs(Role.CONSULTANT);

        assertThatThrownBy(() -> supplierSearchApi.recordLocalDmcBooking(UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aConsultantCannotRecordALocalDmcCancellationDMC04() {
        authenticateAs(Role.CONSULTANT);

        assertThatThrownBy(() -> supplierSearchApi.recordLocalDmcCancellation(UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aConsultantCannotRecordALocalDmcComplaintDMC04() {
        authenticateAs(Role.CONSULTANT);

        assertThatThrownBy(() -> supplierSearchApi.recordLocalDmcComplaint(UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class);
    }

    private static void authenticateAs(Role role) {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), role,
            role == Role.SUPER_ADMIN ? null : UUID.randomUUID());
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
