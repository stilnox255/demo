package de.ingoschindler.demo.adapter.in.rest;

import de.ingoschindler.demo.application.port.in.CreateDemoItemCommand;
import de.ingoschindler.demo.application.port.in.CreateDemoItemUseCase;
import de.ingoschindler.kernel.storage.ObjectStoragePort;
import de.ingoschindler.kernel.storage.ObjectStorageStub;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The HTTP contract end to end: status codes, headers and problem details.
 *
 * <p>Asserts the wire behaviour a client depends on, not the internals — the unit
 * tests next door cover the rules. What can only be checked here is the
 * translation: 201 with a {@code Location}, 409 on a stale version, 404 for
 * another owner, and a signed URL that actually resolves.</p>
 */
@QuarkusTest
class DemoItemsResourceIT {

    static final String OWNER = "it-user";

    @jakarta.inject.Inject
    CreateDemoItemUseCase createDemoItem;

    @InjectMock
    ObjectStoragePort objectStorage;

    @BeforeEach
    void stubStorage() {
        ObjectStorageStub.stubWrites(objectStorage);
    }

    @Test
    @TestSecurity(user = OWNER, roles = "user")
    void createReturns201WithLocationAndTheCreatedItem() {
        given().contentType(ContentType.JSON).body("""
                {"name": "First item", "description": "why it exists"}
                """).when().post("/api/demo-items").then().statusCode(201)
                .header("Location", containsString("/api/demo-items/")).body("id", notNullValue())
                .body("name", is("First item")).body("status", is("DRAFT")).body("version", is(0))
                .body("attachment", nullValue());
    }

    @Test
    @TestSecurity(user = OWNER, roles = "user")
    void unknownIdReturns404ProblemDetails() {
        given().pathParam("id", UUID.randomUUID()).when().get("/api/demo-items/{id}").then().statusCode(404)
                .contentType(containsString("application/problem+json")).body("type", is("urn:starter:error:not-found"))
                .body("status", is(404)).body("instance", notNullValue());
    }

    @Test
    @TestSecurity(user = OWNER, roles = "user")
    void updateWithAStaleVersionReturns409() {
        String id = createItem("Conflict candidate");

        // First writer wins.
        given().contentType(ContentType.JSON).pathParam("id", id).body("""
                {"name": "Winner", "status": "ACTIVE", "expectedVersion": 0}
                """).when().put("/api/demo-items/{id}").then().statusCode(200).body("version", is(1));

        // Second writer still holds version 0.
        given().contentType(ContentType.JSON).pathParam("id", id).body("""
                {"name": "Loser", "status": "ACTIVE", "expectedVersion": 0}
                """).when().put("/api/demo-items/{id}").then().statusCode(409)
                .contentType(containsString("application/problem+json"))
                .body("type", is("urn:starter:error:concurrent-modification"));
    }

    @Test
    @TestSecurity(user = OWNER, roles = "user")
    void updateWithoutAnExpectedVersionIsRejected() {
        String id = createItem("Needs a version");

        given().contentType(ContentType.JSON).pathParam("id", id).body("""
                {"name": "No version", "status": "ACTIVE"}
                """).when().put("/api/demo-items/{id}").then().statusCode(400);
    }

    /**
     * Seeded through the use case rather than over HTTP, because
     * {@code @TestSecurity} fixes one identity per test method — and the point here
     * is a row that belongs to somebody else.
     */
    @Test
    @TestSecurity(user = OWNER, roles = "user")
    void anotherOwnersItemIs404NotForbidden() {
        var foreign = createDemoItem.create(new CreateDemoItemCommand("Not yours", "", "someone-else")).item();

        given().pathParam("id", foreign.id()).when().get("/api/demo-items/{id}").then().statusCode(404);
    }

    /**
     * Zero items means zero pages (ADR-13). A client looping
     * {@code for (p = 1; p <= totalPages; p++)} then does nothing, where clamping to
     * 1 would send it after a page it already knows is empty. Runs as an owner with
     * no rows so the count really is zero.
     */
    @Test
    @TestSecurity(user = "owner-without-items", roles = "user")
    void anEmptyResultReportsZeroTotalPages() {
        given().queryParam("pageSize", 10).when().get("/api/demo-items").then().statusCode(200)
                .body("meta.totalItems", is(0)).body("meta.totalPages", is(0)).body("items", is(java.util.List.of()));
    }

    @Test
    @TestSecurity(user = OWNER, roles = "user")
    void pageSizeIsCappedServerSide() {
        given().queryParam("pageSize", 100_000).when().get("/api/demo-items").then().statusCode(200)
                .body("meta.pageSize", is(100));
    }

    @Test
    @TestSecurity(user = OWNER, roles = "user")
    void attachmentRoundTripsThroughASignedUrl() throws Exception {
        byte[] content = "attachment payload".getBytes();
        when(objectStorage.download(any(UUID.class))).thenReturn(new ByteArrayInputStream(content));

        String id = createItem("With attachment");

        String downloadUrl = given().pathParam("id", id).multiPart("file", "notes.txt", content, "text/plain").when()
                .post("/api/demo-items/{id}/attachment").then().statusCode(200)
                .body("attachment.fileName", is("notes.txt")).body("attachment.contentType", is("text/plain"))
                .body("attachment.size", is(content.length)).body("attachment.downloadUrl", containsString("?t="))
                .extract().path("attachment.downloadUrl");

        assertNotNull(downloadUrl);
        String pathWithToken = downloadUrl.substring(downloadUrl.indexOf("/api/"));

        byte[] downloaded = given().when().get(pathWithToken).then().statusCode(200)
                .header("Content-Disposition", containsString("notes.txt")).extract().asByteArray();

        assertArrayEquals(content, downloaded);
    }

    @Test
    void downloadWithoutAValidTokenIs404NotUnauthorized() {
        given().pathParam("id", UUID.randomUUID()).when().get("/api/demo-items/{id}/attachment").then().statusCode(404);

        given().pathParam("id", UUID.randomUUID()).queryParam("t", "forged").when()
                .get("/api/demo-items/{id}/attachment").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = OWNER, roles = "user")
    void deleteReturns204AndTheItemIsGone() {
        String id = createItem("Doomed");

        given().pathParam("id", id).when().delete("/api/demo-items/{id}").then().statusCode(204);
        given().pathParam("id", id).when().get("/api/demo-items/{id}").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = OWNER, roles = "user")
    void theArchiveTriggerIsAdminOnly() {
        given().when().post("/api/demo-items/archive-stale").then().statusCode(403);
    }

    @Test
    void anonymousCallersAreRejected() {
        given().when().get("/api/demo-items").then().statusCode(401);
    }

    private String createItem(String name) {
        return given().contentType(ContentType.JSON).body("{\"name\": \"" + name + "\"}").when().post("/api/demo-items")
                .then().statusCode(201).extract().path("id");
    }
}
