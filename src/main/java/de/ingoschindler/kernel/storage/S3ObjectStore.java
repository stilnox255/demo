package de.ingoschindler.kernel.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@ApplicationScoped
public class S3ObjectStore {

    @Inject
    S3Client s3;

    /**
     * Upload an object to the given bucket. Returns the S3 key.
     */
    public String put(String bucket, String key, InputStream data, String contentType, long size) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 bucket name is not configured");
        }

        var requestBuilder = PutObjectRequest.builder().bucket(bucket).key(key);

        if (contentType != null && !contentType.isBlank()) {
            requestBuilder.contentType(contentType);
        }

        s3.putObject(requestBuilder.build(), RequestBody.fromInputStream(data, size));
        return key;
    }

    /**
     * Download an object from the given bucket.
     */
    public ResponseInputStream<GetObjectResponse> get(String bucket, String key) {
        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    /**
     * Delete an object from the given bucket.
     */
    public void delete(String bucket, String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
