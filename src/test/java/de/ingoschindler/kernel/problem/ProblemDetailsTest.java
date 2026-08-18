package de.ingoschindler.kernel.problem;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemDetailsTest {

    private final Jsonb jsonb = JsonbBuilder.create();

    private JsonObject toJson(ProblemDetails details) {
        String text = jsonb.toJson(details);
        return Json.createReader(new java.io.StringReader(text)).readObject();
    }

    @Test
    void includes_all_rfc7807_keys() {
        ProblemDetails details = new ProblemDetails(URI.create("about:blank"), "Not Found", 404,
                "Import file not found", URI.create("/organizations/o/account-import/123"), null);

        JsonObject json = toJson(details);

        assertEquals("about:blank", json.getString("type"));
        assertEquals("Not Found", json.getString("title"));
        assertEquals(404, json.getInt("status"));
        assertEquals("Import file not found", json.getString("detail"));
        assertEquals("/organizations/o/account-import/123", json.getString("instance"));
        assertFalse(json.containsKey("correlationId"));
    }

    @Test
    void about_blank_is_the_default_type() {
        ProblemDetails details = ProblemDetails.of(400, "Bad Request", "Missing field", URI.create("/x"));

        assertEquals(URI.create("about:blank"), details.type());
        JsonObject json = toJson(details);
        assertEquals("about:blank", json.getString("type"));
    }

    @Test
    void omits_errors_when_null_or_empty() {
        ProblemDetails details = ProblemDetails.of(400, "Bad Request", "x", URI.create("/x"));
        JsonObject json = toJson(details);

        assertNull(json.get("errors"));
    }

    @Test
    void serialises_validation_field_errors() {
        ProblemDetails details = new ProblemDetails(URI.create("about:blank"), "Bad Request", 400, "Validation failed",
                URI.create("/x"), List.of(new ProblemDetails.FieldError("organizationId", "must not be blank"),
                        new ProblemDetails.FieldError("format", "must not be null")));

        JsonObject json = toJson(details);

        assertTrue(json.containsKey("errors"));
        assertEquals(2, json.getJsonArray("errors").size());
        assertEquals("organizationId", json.getJsonArray("errors").getJsonObject(0).getString("field"));
        assertEquals("must not be blank", json.getJsonArray("errors").getJsonObject(0).getString("message"));
    }
}
