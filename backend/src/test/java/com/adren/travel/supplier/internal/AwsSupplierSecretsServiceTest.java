package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceExistsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FND-11 — proves the secret is written to Secrets Manager and only the ARN comes back. */
@ExtendWith(MockitoExtension.class)
class AwsSupplierSecretsServiceTest {

    @Mock
    SecretsManagerClient secretsManagerClient;

    AwsSupplierSecretsService service;

    @BeforeEach
    void setUp() {
        service = new AwsSupplierSecretsService(secretsManagerClient);
    }

    @Test
    void createsANewSecretAndReturnsItsArnWhenNoneExistsYet() {
        when(secretsManagerClient.createSecret(any(CreateSecretRequest.class)))
            .thenReturn(CreateSecretResponse.builder()
                .arn("arn:aws:secretsmanager:ap-south-1:000000000000:secret:adren/supplier-credentials/HOTELBEDS")
                .build());

        String arn = service.storeSecret(SupplierId.HOTELBEDS, "raw-secret-value");

        assertThat(arn).isEqualTo("arn:aws:secretsmanager:ap-south-1:000000000000:secret:adren/supplier-credentials/HOTELBEDS");
        ArgumentCaptor<CreateSecretRequest> captor = ArgumentCaptor.forClass(CreateSecretRequest.class);
        verify(secretsManagerClient).createSecret(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("adren/supplier-credentials/HOTELBEDS");
        assertThat(captor.getValue().secretString()).isEqualTo("raw-secret-value");
    }

    @Test
    void fallsBackToPutSecretValueWhenTheSecretAlreadyExists() {
        when(secretsManagerClient.createSecret(any(CreateSecretRequest.class)))
            .thenThrow(ResourceExistsException.builder().message("already exists").build());
        when(secretsManagerClient.putSecretValue(any(PutSecretValueRequest.class)))
            .thenReturn(PutSecretValueResponse.builder()
                .arn("arn:aws:secretsmanager:ap-south-1:000000000000:secret:adren/supplier-credentials/HOTELBEDS")
                .build());

        String arn = service.storeSecret(SupplierId.HOTELBEDS, "rotated-secret-value");

        assertThat(arn).isEqualTo("arn:aws:secretsmanager:ap-south-1:000000000000:secret:adren/supplier-credentials/HOTELBEDS");
        ArgumentCaptor<PutSecretValueRequest> captor = ArgumentCaptor.forClass(PutSecretValueRequest.class);
        verify(secretsManagerClient).putSecretValue(captor.capture());
        assertThat(captor.getValue().secretId()).isEqualTo("adren/supplier-credentials/HOTELBEDS");
        assertThat(captor.getValue().secretString()).isEqualTo("rotated-secret-value");
    }

    @Test
    void getSecretValueResolvesTheRawValueByArnDMC07() {
        String arn = "arn:aws:secretsmanager:ap-south-1:000000000000:secret:adren/supplier-credentials/HOTELBEDS";
        when(secretsManagerClient.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(GetSecretValueResponse.builder().secretString("adren-own-secret-value").build());

        assertThat(service.getSecretValue(arn)).isEqualTo("adren-own-secret-value");
        ArgumentCaptor<GetSecretValueRequest> captor = ArgumentCaptor.forClass(GetSecretValueRequest.class);
        verify(secretsManagerClient).getSecretValue(captor.capture());
        assertThat(captor.getValue().secretId()).isEqualTo(arn);
    }
}
