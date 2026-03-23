package com.goaiplatform.backend.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ApiKeyCryptoServiceTest {

    @Test
    void encryptAndDecryptShouldKeepOriginalValue() {
        ApiKeyCryptoService cryptoService = new ApiKeyCryptoService("unit-test-secret");
        String raw = "sk-test-value-123456";
        String encrypted = cryptoService.encrypt(raw);
        String decrypted = cryptoService.decrypt(encrypted);
        Assertions.assertEquals(raw, decrypted);
    }

    @Test
    void fingerprintShouldBeStable() {
        ApiKeyCryptoService cryptoService = new ApiKeyCryptoService("unit-test-secret");
        String first = cryptoService.fingerprint("sk-value");
        String second = cryptoService.fingerprint("sk-value");
        Assertions.assertEquals(first, second);
        Assertions.assertEquals(16, first.length());
    }
}
