package com.agrichain.common.crypto;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AesEncryptionUtilTest {

    // 32-byte (256-bit) key encoded as Base64
    private static final String TEST_KEY =
            Base64.getEncoder().encodeToString(new byte[32]); // all-zero key for tests

    private final AesEncryptionUtil util = new AesEncryptionUtil(TEST_KEY);

    @BeforeEach
    void setUp() {
        // util is initialized as a field; nothing extra needed
    }

    @Test
    void encryptAndDecryptRoundTrip() {
        String plaintext = "John Doe";
        String encrypted = util.encrypt(plaintext);
        assertNotEquals(plaintext, encrypted, "Encrypted value must differ from plaintext");
        assertEquals(plaintext, util.decrypt(encrypted), "Decrypted value must equal original plaintext");
    }

    @Test
    void encryptNullReturnsNull() {
        assertNull(util.encrypt(null));
    }

    @Test
    void decryptNullReturnsNull() {
        assertNull(util.decrypt(null));
    }

    @Test
    void sameInputProducesDifferentCiphertexts() {
        // AES-GCM uses a random IV, so two encryptions of the same plaintext differ
        String plaintext = "test@example.com";
        String enc1 = util.encrypt(plaintext);
        String enc2 = util.encrypt(plaintext);
        assertNotEquals(enc1, enc2, "Each encryption should produce a unique ciphertext due to random IV");
    }

    @Test
    void invalidKeyLengthThrows() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]); // 128-bit — invalid for AES-256
        assertThrows(IllegalArgumentException.class, () -> new AesEncryptionUtil(shortKey));
    }

    // Property: for any non-null string, encrypt then decrypt returns the original
    @Property(tries = 100)
    void encryptDecryptIsIdentity(@ForAll String plaintext) {
        String encrypted = util.encrypt(plaintext);
        assertEquals(plaintext, util.decrypt(encrypted));
    }

    // Property: encrypted value is never equal to plaintext (for non-empty strings)
    @Property(tries = 100)
    void encryptedValueDiffersFromPlaintext(@ForAll @StringLength(min = 1) String plaintext) {
        assertNotEquals(plaintext, util.encrypt(plaintext));
    }
}
