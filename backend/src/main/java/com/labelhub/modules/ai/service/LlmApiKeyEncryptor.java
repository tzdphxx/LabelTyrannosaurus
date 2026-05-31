package com.labelhub.modules.ai.service;

import com.labelhub.common.exception.BusinessException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LlmApiKeyEncryptor {

    private static final int SECRET_NOT_CONFIGURED = 500301;
    private static final int ENCRYPTION_FAILED = 500302;
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private final String secret;
    private final SecureRandom secureRandom = new SecureRandom();

    public LlmApiKeyEncryptor(@Value("${labelhub.llm.key-encryption-secret:}") String secret) {
        this.secret = secret;
    }

    public String encrypt(String plaintext) {
        requireSecret();
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException ex) {
            throw new BusinessException(ENCRYPTION_FAILED, "Failed to encrypt LLM API key");
        }
    }

    public String decrypt(String encryptedValue) {
        requireSecret();
        try {
            byte[] packed = Base64.getDecoder().decode(encryptedValue);
            ByteBuffer buffer = ByteBuffer.wrap(packed);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (RuntimeException | GeneralSecurityException ex) {
            throw new BusinessException(ENCRYPTION_FAILED, "Failed to decrypt LLM API key");
        }
    }

    private SecretKeySpec secretKey() throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return new SecretKeySpec(digest.digest(secret.getBytes(StandardCharsets.UTF_8)), "AES");
    }

    private void requireSecret() {
        if (secret == null || secret.isBlank()) {
            throw new BusinessException(SECRET_NOT_CONFIGURED, "LLM API key encryption secret is not configured");
        }
    }
}
