package com.petrecovery.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class PhoneCryptoUtil {

    private static final String PREFIX = "ENC:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec keySpec;

    public PhoneCryptoUtil(@Value("${phone.crypto.secret:${jwt.secret:pet-recovery-phone-secret}}") String secret) {
        this.keySpec = new SecretKeySpec(deriveKey(secret), "AES");
    }

    public String encrypt(String plainPhone) {
        String normalized = normalize(plainPhone);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith(PREFIX)) {
            return normalized;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new RuntimeException("联系电话加密失败", e);
        }
    }

    public String decrypt(String storedPhone) {
        if (storedPhone == null || storedPhone.isBlank()) {
            return null;
        }
        if (!storedPhone.startsWith(PREFIX)) {
            return storedPhone.trim();
        }
        try {
            byte[] payload = Base64.getDecoder().decode(storedPhone.substring(PREFIX.length()));
            if (payload.length <= IV_LENGTH) {
                return null;
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public String mask(String storedPhone) {
        String plain = decrypt(storedPhone);
        if (plain == null || plain.isBlank()) {
            return null;
        }
        if (plain.length() <= 4) {
            return "****";
        }
        if (plain.length() <= 7) {
            return plain.charAt(0) + "****" + plain.substring(plain.length() - 1);
        }
        return plain.substring(0, 3) + "****" + plain.substring(plain.length() - 4);
    }

    public boolean hasPhone(String storedPhone) {
        String plain = decrypt(storedPhone);
        return plain != null && !plain.isBlank();
    }

    public String normalize(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.trim().replaceAll("[\\s-]", "");
        return normalized.isBlank() ? null : normalized;
    }

    private byte[] deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Arrays.copyOf(digest.digest(secret.getBytes(StandardCharsets.UTF_8)), 16);
        } catch (Exception e) {
            throw new RuntimeException("联系电话密钥初始化失败", e);
        }
    }
}
