package de.ingoschindler.kernel.hash;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Sha256Test {

    /** Known-answer vector: SHA-256 of "abc", per FIPS 180-4. */
    private static final String ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @Test
    void hexOfBytesMatchesTheKnownVector() {
        assertEquals(ABC, Sha256.hex("abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void hexOfAStreamMatchesTheSameVector() {
        assertEquals(ABC, Sha256.hex(new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void streamingLargerThanOneBufferStillHashesEveryByte() {
        var data = new byte[64 * 1024 + 7];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }

        assertEquals(Sha256.hex(data), Sha256.hex(new ByteArrayInputStream(data)));
    }

    @Test
    void emptyInputHashesToTheEmptyDigest() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Sha256.hex(new byte[0]));
    }
}
