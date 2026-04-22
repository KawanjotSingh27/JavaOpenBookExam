package com.portal.encryption;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

// ──────────────────────────────────────────────────────────────────────────────
// EncryptionStrategy — Strategy Interface
// ──────────────────────────────────────────────────────────────────────────────

/**
 * EncryptionStrategy — the Strategy interface for file encryption.
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  STRATEGY PATTERN JUSTIFICATION                                          │
 * │                                                                          │
 * │  Different study materials carry different sensitivity levels.           │
 * │  A syllabus PDF (public) needs no encryption; an exam paper (private)   │
 * │  needs strong AES-256 encryption.  The Strategy pattern lets the        │
 * │  FileUploadService apply the correct algorithm at runtime without       │
 * │  if/else chains scattered across the codebase.  Swapping algorithms     │
 * │  (e.g., adding ChaCha20) requires only a new strategy class.           │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
public interface EncryptionStrategy {

    /**
     * Encrypts raw file bytes and returns the ciphertext.
     *
     * @param data Plain-text bytes to encrypt.
     * @return Encrypted bytes (or the original bytes if strategy is NONE).
     */
    byte[] encrypt(byte[] data);

    /**
     * Human-readable description of the encryption level (for metadata).
     *
     * @return e.g. "NONE", "AES-128", "AES-256".
     */
    String levelDescription();
}


// ──────────────────────────────────────────────────────────────────────────────
// Concrete Strategy 1: No encryption (public / open-access materials)
// ──────────────────────────────────────────────────────────────────────────────

/**
 * NoEncryption — pass-through strategy for publicly accessible study materials.
 * Zero CPU overhead; no keys to manage.
 */
class NoEncryption implements EncryptionStrategy {

    @Override
    public byte[] encrypt(byte[] data) {
        System.out.println("[Encryption] Level: NONE — file stored as-is.");
        return data;  // no transformation
    }

    @Override
    public String levelDescription() { return "NONE"; }
}


// ──────────────────────────────────────────────────────────────────────────────
// Concrete Strategy 2: AES-128 (internal / faculty-only materials)
// ──────────────────────────────────────────────────────────────────────────────

/**
 * AES128Encryption — symmetric AES with a 128-bit key.
 * Uses only JDK-bundled javax.crypto — no proprietary libraries.
 */
class AES128Encryption implements EncryptionStrategy {

    // 16-byte key = 128-bit AES (demo key; in production load from a key store)
    private static final byte[] KEY_128 = "OpenJDKAES128Key".getBytes();

    @Override
    public byte[] encrypt(byte[] data) {
        System.out.println("[Encryption] Level: AES-128 — encrypting file content.");
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY_128, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data);
            // Encode to Base64 for safe binary-to-text storage
            return Base64.getEncoder().encode(encrypted);
        } catch (Exception e) {
            System.err.println("[Encryption] AES-128 failed: " + e.getMessage());
            return data; // fallback — return original data (log the error)
        }
    }

    @Override
    public String levelDescription() { return "AES-128"; }
}


// ──────────────────────────────────────────────────────────────────────────────
// Concrete Strategy 3: AES-256 (confidential / exam materials)
// ──────────────────────────────────────────────────────────────────────────────

/**
 * AES256Encryption — symmetric AES with a 256-bit key (maximum JDK strength).
 * Suitable for exam papers, model answers, and sensitive academic content.
 */
class AES256Encryption implements EncryptionStrategy {

    // 32-byte key = 256-bit AES (demo key; use java.security.KeyStore in production)
    private static final byte[] KEY_256 = "OpenJDKAES256KeyForSecurePortal!".getBytes();

    @Override
    public byte[] encrypt(byte[] data) {
        System.out.println("[Encryption] Level: AES-256 — applying strong encryption.");
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY_256, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data);
            return Base64.getEncoder().encode(encrypted);
        } catch (Exception e) {
            System.err.println("[Encryption] AES-256 failed: " + e.getMessage());
            return data;
        }
    }

    @Override
    public String levelDescription() { return "AES-256"; }
}


// ──────────────────────────────────────────────────────────────────────────────
// EncryptionFactory — resolves a strategy from a string label (used with DI)
// ──────────────────────────────────────────────────────────────────────────────

/**
 * EncryptionFactory provides the correct EncryptionStrategy based on a
 * configured security level string (e.g., from a property file or DB record).
 */
class EncryptionFactory {

    public static EncryptionStrategy getStrategy(String level) {
        return switch (level.toUpperCase()) {
            case "NONE"    -> new NoEncryption();
            case "AES-128" -> new AES128Encryption();
            case "AES-256" -> new AES256Encryption();
            default        -> throw new IllegalArgumentException("Unknown encryption level: " + level);
        };
    }
}
