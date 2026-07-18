package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceExistsException;

/**
 * FND-11 — one Secrets Manager secret per supplier, named
 * {@code adren/supplier-credentials/<SUPPLIER_ID>}. {@code createSecret}
 * fails with {@link ResourceExistsException} once the secret already
 * exists (e.g. a later credential rotation), which is the SDK's own signal
 * to fall back to {@code putSecretValue} instead — there is no
 * "upsert" operation in the Secrets Manager API.
 */
@Service
class AwsSupplierSecretsService implements SupplierSecretsService {

    private static final String SECRET_NAME_PREFIX = "adren/supplier-credentials/";

    private final SecretsManagerClient secretsManagerClient;

    AwsSupplierSecretsService(SecretsManagerClient secretsManagerClient) {
        this.secretsManagerClient = secretsManagerClient;
    }

    @Override
    public String storeSecret(SupplierId supplierId, String secretValue) {
        String secretName = SECRET_NAME_PREFIX + supplierId.name();
        try {
            return secretsManagerClient.createSecret(CreateSecretRequest.builder()
                .name(secretName)
                .secretString(secretValue)
                .build()).arn();
        } catch (ResourceExistsException alreadyExists) {
            return secretsManagerClient.putSecretValue(PutSecretValueRequest.builder()
                .secretId(secretName)
                .secretString(secretValue)
                .build()).arn();
        }
    }

    @Override
    public String getSecretValue(String secretArn) {
        return secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
            .secretId(secretArn)
            .build()).secretString();
    }
}
