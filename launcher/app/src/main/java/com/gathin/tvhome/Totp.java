package com.gathin.tvhome;

import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** RFC 6238 / RFC 4226, SHA-1, 30-second steps. No clock or Android dependency in the verifier. */
final class Totp {
    static String code(byte[] secret, long step, int digits) throws GeneralSecurityException {
        byte[] counter = new byte[8];
        for (int i = 7; i >= 0; i--) { counter[i] = (byte) step; step >>>= 8; }
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secret, "HmacSHA1"));
        byte[] hash = mac.doFinal(counter);
        int offset = hash[hash.length - 1] & 15;
        int value = (hash[offset] & 127) << 24 | (hash[offset + 1] & 255) << 16
                | (hash[offset + 2] & 255) << 8 | hash[offset + 3] & 255;
        int modulus = digits == 8 ? 100000000 : 1000000;
        String number = Integer.toString(value % modulus);
        return "00000000".substring(0, digits - number.length()) + number;
    }

    static long match(byte[] secret, String entered, long seconds, long lastStep)
            throws GeneralSecurityException {
        if (secret.length != 20 || seconds < 0 || entered == null || !entered.matches("[0-9]{6}")) return -1;
        long current = seconds / 30;
        for (long step = current - 1; step <= current + 1; step++) {
            if (step < 0 || step <= lastStep) continue;
            String expected = code(secret, step, 6);
            int difference = 0;
            for (int i = 0; i < 6; i++) difference |= entered.charAt(i) ^ expected.charAt(i);
            if (difference == 0) return step;
        }
        return -1;
    }

    private Totp() {}
}
