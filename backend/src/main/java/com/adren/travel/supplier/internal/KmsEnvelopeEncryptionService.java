package com.adren.travel.supplier.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * FND-12 / RULES.md §5.3 / PRD §10.4 — KMS envelope encryption for BYOS
 * credentials: KMS generates and wraps a one-time AES-256 data key per
 * encryption call (never persisted in plaintext); the actual credential
 * bytes are encrypted locally with that plaintext key via AES-256-GCM and
 * KMS never sees the credential itself, only ever the data key. This is
 * the standard AWS envelope-encryption pattern — distinct from FND-11's
 * "whole secret lives in Secrets Manager, only the ARN in Postgres"
 * pattern, since BYOS credentials are row-level/per-Consultant rather
 * than one-secret-per-supplier.
 */
@Service
class KmsEnvelopeEncryptionService {

    private static final String AES_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final KmsClient kmsClient;
    private final String kmsKeyId;
    private final SecureRandom secureRandom = new SecureRandom();

    KmsEnvelopeEncryptionService(KmsClient kmsClient, @Value("${adren.aws.kms-key-id}") String kmsKeyId) {
        this.kmsClient = kmsClient;
        this.kmsKeyId = kmsKeyId;
    }

    /** The three values {@link ByosCredential} persists per row — none of them the plaintext credential. */
    record EncryptedPayload(byte[] ciphertext, byte[] iv, byte[] wrappedDataKey) {
    }

    EncryptedPayload encrypt(String plaintext) {
        GenerateDataKeyResponse dataKey = kmsClient.generateDataKey(GenerateDataKeyRequest.builder()
            .keyId(kmsKeyId)
            .keySpec(DataKeySpec.AES_256)
            .build());
        byte[] plaintextDataKey = dataKey.plaintext().asByteArray();
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(plaintextDataKey, AES_ALGORITHM),
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new EncryptedPayload(ciphertext, iv, dataKey.ciphertextBlob().asByteArray());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt BYOS credential", e);
        }
    }

    String decrypt(EncryptedPayload payload) {
        byte[] plaintextDataKey = kmsClient.decrypt(DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(payload.wrappedDataKey()))
                .build())
            .plaintext().asByteArray();
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(plaintextDataKey, AES_ALGORITHM),
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.iv()));
            byte[] plaintext = cipher.doFinal(payload.ciphertext());
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt BYOS credential", e);
        }
    }
}
