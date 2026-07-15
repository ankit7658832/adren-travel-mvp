package com.adren.travel.supplier.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FND-12 — proves the encrypt/decrypt round-trip against a mocked KMS
 * (unit tier per the story's testing_tiers; the real-LocalStack-KMS tier
 * is {@code ByosCredentialCrossTenantIT}). The AES-GCM local encryption
 * itself is real, not mocked — only KMS's GenerateDataKey/Decrypt calls
 * are, since those are the network boundary this class owns.
 */
@ExtendWith(MockitoExtension.class)
class KmsEnvelopeEncryptionServiceTest {

    private static final String KEY_ID = "alias/test-key";

    @Mock
    KmsClient kmsClient;

    private KmsEnvelopeEncryptionService service;
    private byte[] plaintextDataKey;

    @BeforeEach
    void setUp() {
        service = new KmsEnvelopeEncryptionService(kmsClient, KEY_ID);
        plaintextDataKey = new byte[32];
        new SecureRandom().nextBytes(plaintextDataKey);
    }

    @Test
    void encryptsAndDecryptsBackToTheOriginalPlaintext() {
        byte[] wrappedDataKey = "wrapped-key-blob".getBytes();
        when(kmsClient.generateDataKey(any(GenerateDataKeyRequest.class))).thenReturn(GenerateDataKeyResponse.builder()
            .plaintext(SdkBytes.fromByteArray(plaintextDataKey))
            .ciphertextBlob(SdkBytes.fromByteArray(wrappedDataKey))
            .build());
        when(kmsClient.decrypt(any(DecryptRequest.class))).thenReturn(DecryptResponse.builder()
            .plaintext(SdkBytes.fromByteArray(plaintextDataKey))
            .build());

        KmsEnvelopeEncryptionService.EncryptedPayload encrypted = service.encrypt("hotelbeds-api-key-12345");
        String decrypted = service.decrypt(encrypted);

        assertThat(decrypted).isEqualTo("hotelbeds-api-key-12345");
        assertThat(encrypted.wrappedDataKey()).isEqualTo(wrappedDataKey);
    }

    @Test
    void requestsTheConfiguredKmsKeyIdForEveryEncryption() {
        when(kmsClient.generateDataKey(any(GenerateDataKeyRequest.class))).thenReturn(GenerateDataKeyResponse.builder()
            .plaintext(SdkBytes.fromByteArray(plaintextDataKey))
            .ciphertextBlob(SdkBytes.fromByteArray("wrapped".getBytes()))
            .build());

        service.encrypt("some-secret");

        var captor = org.mockito.ArgumentCaptor.forClass(GenerateDataKeyRequest.class);
        verify(kmsClient).generateDataKey(captor.capture());
        assertThat(captor.getValue().keyId()).isEqualTo(KEY_ID);
    }

    @Test
    void neverStoresThePlaintextCredentialInTheCiphertextField() {
        when(kmsClient.generateDataKey(any(GenerateDataKeyRequest.class))).thenReturn(GenerateDataKeyResponse.builder()
            .plaintext(SdkBytes.fromByteArray(plaintextDataKey))
            .ciphertextBlob(SdkBytes.fromByteArray("wrapped".getBytes()))
            .build());

        KmsEnvelopeEncryptionService.EncryptedPayload encrypted = service.encrypt("super-secret-raw-value");

        assertThat(new String(encrypted.ciphertext())).doesNotContain("super-secret-raw-value");
    }

    @Test
    void differentEncryptionsOfTheSamePlaintextProduceDifferentCiphertextDueToRandomIv() {
        when(kmsClient.generateDataKey(any(GenerateDataKeyRequest.class))).thenReturn(GenerateDataKeyResponse.builder()
            .plaintext(SdkBytes.fromByteArray(plaintextDataKey))
            .ciphertextBlob(SdkBytes.fromByteArray("wrapped".getBytes()))
            .build());

        KmsEnvelopeEncryptionService.EncryptedPayload first = service.encrypt("same-secret");
        KmsEnvelopeEncryptionService.EncryptedPayload second = service.encrypt("same-secret");

        assertThat(first.iv()).isNotEqualTo(second.iv());
        assertThat(first.ciphertext()).isNotEqualTo(second.ciphertext());
    }
}
