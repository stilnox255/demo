package de.ingoschindler.kernel.download;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * HMAC-SHA256 implementation of {@link DownloadTokenPort}.
 * Token format: base64url(resourceId|expiry|base64url(hmacSha256(resourceId|expiry)))
 *
 * <p>Comparison goes through {@link MessageDigest#isEqual} rather than
 * {@code String.equals} so a forged token cannot be refined byte by byte off
 * the response time.</p>
 */
@ApplicationScoped
public class DownloadTokens implements DownloadTokenPort {

    private static final Logger LOGGER = Logger.getLogger(DownloadTokens.class);
    private static final String ALGORITHM = "HmacSHA256";

    @ConfigProperty(name = "starter.download.token.secret")
    String secret;

    @ConfigProperty(name = "starter.download.token.ttl-seconds", defaultValue = "300")
    int ttlSeconds;

    public String generate(UUID resourceId) {
        long expiry = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = resourceId + "|" + expiry;
        String hmac = sign(payload);
        String fullPayload = payload + "|" + hmac;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(fullPayload.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validate(String token, UUID resourceId) {
        try {
            var decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            var parts = decoded.split("\\|", 3);
            if (parts.length != 3)
                return false;

            var tokenResourceId = UUID.fromString(parts[0]);
            long expiry = Long.parseLong(parts[1]);
            var tokenHmac = parts[2];

            if (!tokenResourceId.equals(resourceId))
                return false;
            if (Instant.now().getEpochSecond() > expiry)
                return false;

            var expectedHmac = sign(parts[0] + "|" + parts[1]);
            return MessageDigest.isEqual(tokenHmac.getBytes(StandardCharsets.UTF_8),
                    expectedHmac.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.debugf("Token validation failed: %s", e.getMessage());
            return false;
        }
    }

    public String signedUrl(String baseUrl, String path, UUID resourceId) {
        return baseUrl + path + "?t=" + generate(resourceId);
    }

    private String sign(String payload) {
        try {
            var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            var mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            var hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA256 not available", e);
        }
    }
}
