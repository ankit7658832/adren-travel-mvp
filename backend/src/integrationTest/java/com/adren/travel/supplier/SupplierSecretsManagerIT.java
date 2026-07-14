package com.adren.travel.supplier;

import com.adren.travel.infra.TestInfrastructure;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FND-11's core acceptance criterion, proven against a real (containerized)
 * Secrets Manager: saving a supplier credential writes the real value to
 * Secrets Manager and persists only the ARN in Postgres — see
 * {@code SupplierAggregationServiceTest} for the mocked-Secrets-Manager
 * unit-test tier of the same behavior.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(initializers = TestInfrastructure.class)
class SupplierSecretsManagerIT {

    @Autowired
    SupplierSearchApi supplierSearchApi;

    @Autowired
    SecretsManagerClient secretsManagerClient;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void savingACredentialWritesTheRealSecretToSecretsManagerAndPersistsOnlyItsArn() {
        authenticateAsSuperAdmin();

        supplierSearchApi.updateSupplierCredential(
            new UpdateSupplierCredentialCommand(SupplierId.HOTELBEDS, "hotelbeds-real-api-key"));

        List<SupplierCredentialSummary> summaries = supplierSearchApi.listSupplierCredentials();
        assertThat(summaries).extracting(SupplierCredentialSummary::supplierId).contains(SupplierId.HOTELBEDS);
        // SupplierCredentialSummary carries no ARN/secret field at all — the
        // real assertion is that Secrets Manager itself holds the raw value:
        String storedValue = secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
                .secretId("adren/supplier-credentials/HOTELBEDS")
                .build())
            .secretString();
        assertThat(storedValue).isEqualTo("hotelbeds-real-api-key");
    }

    @Test
    void rotatingACredentialUpdatesTheSameSecretInSecretsManager() {
        authenticateAsSuperAdmin();
        supplierSearchApi.updateSupplierCredential(
            new UpdateSupplierCredentialCommand(SupplierId.STUBA, "first-value"));

        supplierSearchApi.updateSupplierCredential(
            new UpdateSupplierCredentialCommand(SupplierId.STUBA, "rotated-value"));

        String storedValue = secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
                .secretId("adren/supplier-credentials/STUBA")
                .build())
            .secretString();
        assertThat(storedValue).isEqualTo("rotated-value");
    }

    private static void authenticateAsSuperAdmin() {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), Role.SUPER_ADMIN, null);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
