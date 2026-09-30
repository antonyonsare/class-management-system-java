package com.cdb.cms.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Offline stand-in for Firebase Auth: salted SHA-256 password hashing.
 *
 * <p>Each hash uses a fresh 128-bit random salt, stored together with the
 * digest in the format {@code <saltHex>:<digestHex>}. Verification recomputes
 * the digest over {@code salt + password} and compares in constant time.
 *
 * <p>Integration note: a future Firestore adapter delegates authentication to
 * Firebase, so {@code passwordHash} is persisted but never interpreted there;
 * this class remains the offline-only credential mechanism.
 */
public final class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;

    private PasswordHasher() {
    }

    public static String hashPassword(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] digest = digest(salt, password);
        return toHex(salt) + ":" + toHex(digest);
    }

    public static boolean verify(char[] password, String stored) {
        if (stored == null || !stored.contains(":")) {
            return false;
        }
        String[] parts = stored.split(":", 2);
        byte[] salt = fromHex(parts[0]);
        byte[] expected = fromHex(parts[1]);
        byte[] actual = digest(salt, password);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] digest(byte[] salt, char[] password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(new String(password).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
