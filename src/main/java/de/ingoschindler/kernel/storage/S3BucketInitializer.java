package de.ingoschindler.kernel.storage;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ApplicationScoped
public class S3BucketInitializer {

    private static final Logger LOGGER = Logger.getLogger(S3BucketInitializer.class);

    @Inject
    S3Client s3;

    @ConfigProperty(name = "s3.bucket.files")
    String filesBucket;

    @ConfigProperty(name = "quarkus.s3.aws.region")
    String awsRegion;

    void onStart(@Observes @Priority(10) StartupEvent ev) {
        initBucket(filesBucket);
    }

    void initBucket(String bucket) {
        try {
            if (bucketExists(bucket)) {
                LOGGER.info("Bucket already exists: " + bucket);
                return;
            }
            createBucket(bucket, Region.of(awsRegion));
            LOGGER.info("Created bucket: " + bucket);
        } catch (S3Exception e) {
            LOGGER.error("Failed to initialize S3 bucket " + bucket + ": "
                    + (e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage()), e);
            throw new RuntimeException("Failed to initialize S3 bucket", e);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize S3 bucket " + bucket + ": " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize S3 bucket", e);
        }
    }

    boolean bucketExists(String bucket) {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    void createBucket(String bucket, Region region) {
        CreateBucketRequest.Builder req = CreateBucketRequest.builder().bucket(bucket);

        if (!Region.US_EAST_1.equals(region)) {
            req.createBucketConfiguration(CreateBucketConfiguration.builder().locationConstraint(region.id()).build());
        }

        s3.createBucket(req.build());
    }
}
