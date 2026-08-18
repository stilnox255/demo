package de.ingoschindler.kernel.download;

import java.util.UUID;

/**
 * Outbound port for signed, time-limited GET access to a protected resource
 * without an {@code Authorization} header (ADR-19).
 *
 * <p>Needed because a browser cannot attach an auth header to an
 * {@code <img src>} or a plain download link. The token travels in the query
 * string instead and carries its own expiry and resource binding, so a leaked
 * URL grants access to exactly one resource for exactly the remaining TTL.</p>
 */
public interface DownloadTokenPort {

    /**
     * Mint a short-lived token scoped to {@code resourceId}.
     */
    String generate(UUID resourceId);

    /**
     * Verify {@code token} was minted for {@code resourceId} and has not expired.
     */
    boolean validate(String token, UUID resourceId);

    /**
     * Build an absolute, signed URL: {@code baseUrl + path + "?t=" + token}.
     * The caller owns {@code path} — this port knows how to sign a URL, not
     * which endpoints exist.
     */
    String signedUrl(String baseUrl, String path, UUID resourceId);
}
