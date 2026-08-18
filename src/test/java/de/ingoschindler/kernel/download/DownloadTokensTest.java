package de.ingoschindler.kernel.download;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The token has to bind to one resource and one time window, or it is just a
 * password in a URL (ADR-19).
 */
class DownloadTokensTest {

    DownloadTokens tokens;

    @BeforeEach
    void setup() {
        tokens = new DownloadTokens();
        tokens.secret = "test-secret-key";
        tokens.ttlSeconds = 300;
    }

    @Test
    void validRoundTrip() {
        var id = UUID.randomUUID();
        var token = tokens.generate(id);
        assertTrue(tokens.validate(token, id));
    }

    @Test
    void expiredTokenRejected() {
        tokens.ttlSeconds = -1;
        var id = UUID.randomUUID();
        var token = tokens.generate(id);
        assertFalse(tokens.validate(token, id));
    }

    @Test
    void tokenForAnotherResourceIsRejected() {
        var id1 = UUID.randomUUID();
        var id2 = UUID.randomUUID();
        var token = tokens.generate(id1);
        assertFalse(tokens.validate(token, id2));
    }

    @Test
    void tamperedTokenIsRejected() {
        var id = UUID.randomUUID();
        var token = tokens.generate(id);

        // Flip a character of the signature *inside* the payload and re-encode, so the
        // token still decodes into a well-formed `id|expiry|signature` and the HMAC is the
        // only thing wrong with it.
        //
        // Not by editing the outer base64: the payload is 91 bytes, so its last character
        // carries two significant bits, and flipping it decodes to the same bytes about a
        // fifth of the time — the token then verifies and this test fails at random.
        var decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        var parts = decoded.split("\\|", 3);
        var signature = parts[2];
        var forged = (signature.charAt(0) == 'A' ? 'B' : 'A') + signature.substring(1);
        var tampered = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((parts[0] + "|" + parts[1] + "|" + forged).getBytes(StandardCharsets.UTF_8));

        assertFalse(tokens.validate(tampered, id));
    }

    @Test
    void garbageIsRejectedWithoutThrowing() {
        assertFalse(tokens.validate("not-base64-at-all", UUID.randomUUID()));
        assertFalse(tokens.validate("", UUID.randomUUID()));
    }

    @Test
    void signedUrlCarriesTheTokenAndThePathTheCallerAsked() {
        var id = UUID.randomUUID();

        var url = tokens.signedUrl("https://example.test", "/api/demo-items/" + id + "/attachment", id);

        assertTrue(url.startsWith("https://example.test/api/demo-items/" + id + "/attachment?t="));
        assertTrue(tokens.validate(url.substring(url.indexOf("?t=") + 3), id));
    }
}
