package de.ingoschindler.kernel.download;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        // Flip the last character of the signature. Without the HMAC check this
        // would still decode into a well-formed payload.
        var tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

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
