package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.labelhub.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

/**
 * E2E 验证用例（阶段 1）：LLM API Key 加解密。
 * 不依赖 Spring 容器，直接用构造器注入密钥，验证 AES/GCM 加密器的核心性质。
 */
class LlmApiKeyEncryptorE2ETest {

    private static final String SECRET = "labelhub-local-llm-key-encryption-secret-change-me";

    @Test
    void encryptThenDecryptRoundTrips() {
        LlmApiKeyEncryptor encryptor = new LlmApiKeyEncryptor(SECRET);
        String plaintext = "sk-PLAINTEXT-SECRET-0607-abcdef123456";

        String encrypted = encryptor.encrypt(plaintext);

        assertThat(encrypted).isNotBlank().isNotEqualTo(plaintext);
        assertThat(encryptor.decrypt(encrypted)).isEqualTo(plaintext);
    }

    @Test
    void samePlaintextEncryptedTwiceDiffersDueToRandomIv() {
        LlmApiKeyEncryptor encryptor = new LlmApiKeyEncryptor(SECRET);
        String plaintext = "sk-same-input-value";

        String first = encryptor.encrypt(plaintext);
        String second = encryptor.encrypt(plaintext);

        assertThat(first).isNotEqualTo(second);
        assertThat(encryptor.decrypt(first)).isEqualTo(plaintext);
        assertThat(encryptor.decrypt(second)).isEqualTo(plaintext);
    }

    @Test
    void decryptWithWrongSecretFails() {
        String ciphertext = new LlmApiKeyEncryptor(SECRET).encrypt("sk-secret");
        LlmApiKeyEncryptor wrongKey = new LlmApiKeyEncryptor("a-totally-different-secret-value");

        assertThatThrownBy(() -> wrongKey.decrypt(ciphertext))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(500302);
    }

    @Test
    void missingSecretThrowsConfigError() {
        LlmApiKeyEncryptor noSecret = new LlmApiKeyEncryptor("");

        assertThatThrownBy(() -> noSecret.encrypt("sk-x"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(500301);
        assertThatThrownBy(() -> noSecret.decrypt("anything"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(500301);
    }
}
