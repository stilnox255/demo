package de.ingoschindler.infrastructure.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

/**
 * Readiness probe for the object store: HEADs the configured bucket, which proves both that the S3
 * endpoint answers and that the bucket the application actually writes to exists.
 *
 * <p>quarkus-amazon-s3 ships no health check of its own, so this is the only S3 readiness signal.
 * The bucket is read from {@code s3.bucket.files} — the same key every other S3 consumer uses —
 * with no default, so a deployment that forgets to configure it fails at startup instead of
 * reporting healthy about a bucket it never touches.
 *
 * <p>{@code @Readiness} and not {@code @Wellness} on purpose: storing and serving files is a
 * core capability here, so an unreachable object store means this instance cannot do its job and
 * should leave the load balancer rotation (ADR-21). Contrast the cache, which is deliberately
 * absent from readiness — a degraded cache still serves correct answers, just slower.
 */
@Readiness
@ApplicationScoped
class S3HealthCheck implements HealthCheck {

    @Inject
    S3Client s3Client;

    @ConfigProperty(name = "s3.bucket.files")
    String bucketName;

    @Override
    public HealthCheckResponse call() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return HealthCheckResponse.up("S3 storage");
        } catch (Exception _) {
            return HealthCheckResponse.down("S3 storage");
        }
    }
}
