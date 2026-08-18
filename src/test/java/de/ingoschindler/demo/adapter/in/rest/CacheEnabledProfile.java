package de.ingoschindler.demo.adapter.in.rest;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Turns the cache back on for the tests that are about the cache.
 *
 * <p>The suite runs with {@code quarkus.cache.enabled=false} so a snapshot cannot
 * survive into the next test class. A behaviour that is disabled everywhere still
 * needs covering somewhere, and a profile is the cheapest way to get exactly one
 * context where it is live.</p>
 */
public class CacheEnabledProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.cache.enabled", "true");
    }
}
