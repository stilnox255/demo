package de.ingoschindler.kernel.hash;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Lowercase-hex SHA-256, the content hash every {@code storage_ref} row is
 * keyed by.
 *
 * <p>Four use cases computed this themselves, two of them hand-rolling the
 * read-into-buffer loop that {@link DigestInputStream} has covered since Java
 * 9. {@code SHA-256} is mandatory in every JDK, so its
 * {@link NoSuchAlgorithmException} is not a runtime condition callers can
 * handle — it is wrapped here once instead of in every caller.
 */
public final class Sha256 {

    private Sha256() {
    }

    public static String hex(byte[] data) {
        return HexFormat.of().formatHex(digest().digest(data));
    }

    /** Consumes {@code data} to the end. The caller still owns closing it. */
    public static String hex(InputStream data) {
        var stream = new DigestInputStream(data, digest());
        try {
            stream.transferTo(OutputStream.nullOutputStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return HexFormat.of().formatHex(stream.getMessageDigest().digest());
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
