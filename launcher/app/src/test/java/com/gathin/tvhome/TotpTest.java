package com.gathin.tvhome;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class TotpTest {
    private final byte[] key = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    @Test public void rfc6238Sha1VectorsIncludingBeyond2038() throws Exception {
        long[] times = {59, 1111111109, 1111111111, 1234567890, 2000000000, 20000000000L};
        String[] expected = {"94287082", "07081804", "14050471", "89005924", "69279037", "65353130"};
        for (int i = 0; i < times.length; i++) {
            assertEquals(expected[i], Totp.code(key, times[i] / 30, 8));
            assertEquals(expected[i].substring(2), Totp.code(key, times[i] / 30, 6));
        }
    }

    @Test public void acceptsOnlyAdjacentSteps() throws Exception {
        for (int offset = -2; offset <= 2; offset++) {
            long result = Totp.match(key, Totp.code(key, 100 + offset, 6), 3000, -1);
            assertEquals(Math.abs(offset) <= 1 ? 100 + offset : -1, result);
        }
    }

    @Test public void usedAndOlderCodesCannotUnlockAgainEvenIfClockMovesBack() throws Exception {
        String code = Totp.code(key, 100, 6);
        assertEquals(100, Totp.match(key, code, 3000, -1));
        assertEquals(-1, Totp.match(key, code, 3000, 100));
        assertEquals(-1, Totp.match(key, code, 2970, 100));
        assertEquals(101, Totp.match(key, Totp.code(key, 101, 6), 3030, 100));
    }

    @Test public void malformedInputAndMissingKeysFailClosed() throws Exception {
        for (String code : new String[]{null, "", "12345", "1234567", "１２３４５６", " 287082", "287082\n"})
            assertEquals(-1, Totp.match(key, code, 59, -1));
        assertEquals(-1, Totp.match(new byte[0], "287082", 59, -1));
        assertEquals(-1, Totp.match(new byte[19], "287082", 59, -1));
        assertEquals(-1, Totp.match(key, "287082", -1, -1));
    }
}
