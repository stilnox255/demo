package de.ingoschindler.infrastructure.web;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard for the HTTP/1.0-without-Host case: Vert.x can only build an absolute URI from a Host
 * header, so {@code HttpServerRequest.absoluteURI()} is null for such a request and RESTEasy Reactive's
 * {@code UriInfo.getRequestUri()} threw a NullPointerException on it. Every exception mapper reads the URI for
 * the problem body's {@code instance} member, so the framework's 404 turned into a 500 that logged the NPE
 * instead of the real cause — which is exactly how the container health probe's 404 was masked.
 * {@link RequestUris} degrades to a null {@code instance} instead.
 *
 * <p>Raw socket rather than RestAssured: no HTTP client will omit the Host header.
 */
@QuarkusTest
class UnmatchedPathProblemIT {

    @TestHTTPResource("/")
    URL baseUrl;

    @Test
    void unmatchedPathWithoutHostHeaderYields404() throws Exception {
        String statusLine = get10WithoutHost("/q/no-such-endpoint");
        assertTrue(statusLine.contains("404"), "expected a 404 status line, got: " + statusLine);
    }

    private String get10WithoutHost(String path) throws Exception {
        try (Socket socket = new Socket(baseUrl.getHost(), baseUrl.getPort())) {
            OutputStream out = socket.getOutputStream();
            out.write(("GET " + path + " HTTP/1.0\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            return reader.readLine();
        }
    }
}
