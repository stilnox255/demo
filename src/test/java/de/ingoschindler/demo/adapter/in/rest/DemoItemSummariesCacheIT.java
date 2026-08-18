package de.ingoschindler.demo.adapter.in.rest;

import de.ingoschindler.demo.application.port.in.CreateDemoItemCommand;
import de.ingoschindler.demo.application.port.in.CreateDemoItemUseCase;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The cached summary endpoint: conditional GETs and invalidation on write
 * (ADR-41).
 *
 * <p>Two properties are worth pinning, because getting either wrong is invisible
 * in normal use. An unchanged list must answer 304 with no body, and a write must
 * move the ETag — a cache that keeps serving the previous answer looks perfectly
 * healthy right up to the support ticket.</p>
 */
@QuarkusTest
@TestProfile(CacheEnabledProfile.class)
class DemoItemSummariesCacheIT {

    static final String OWNER = "cache-user";

    @Inject
    CreateDemoItemUseCase createDemoItem;

    @Test
    @TestSecurity(user = OWNER, roles = "user")
    void unchangedSummariesAnswer304WithoutABody() {
        createDemoItem.create(new CreateDemoItemCommand("cached item", "", OWNER));

        String etag = given().when().get("/api/demo-items/summary").then().statusCode(200)
                .header("ETag", notNullValue()).extract().header("ETag");

        var body = given().header("If-None-Match", etag).when().get("/api/demo-items/summary").then().statusCode(304)
                .extract().asString();

        org.junit.jupiter.api.Assertions.assertTrue(body.isEmpty(), "a 304 carries no body");
    }

    @Test
    @TestSecurity(user = OWNER, roles = "user")
    void aWriteMovesTheEtag() {
        String before = given().when().get("/api/demo-items/summary").then().statusCode(200).extract().header("ETag");

        given().contentType(ContentType.JSON).body("""
                {"name": "invalidates the cache"}
                """).when().post("/api/demo-items").then().statusCode(201);

        String after = given().when().get("/api/demo-items/summary").then().statusCode(200).extract().header("ETag");

        assertNotEquals(before, after, "a write must invalidate the snapshot, not wait for a TTL");
    }

    @Test
    @TestSecurity(user = "other-cache-user", roles = "user")
    void theSnapshotIsScopedToItsOwner() {
        createDemoItem.create(new CreateDemoItemCommand("belongs to someone else", "", OWNER));

        given().when().get("/api/demo-items/summary").then().statusCode(200).body("size()", is(0));
    }
}
