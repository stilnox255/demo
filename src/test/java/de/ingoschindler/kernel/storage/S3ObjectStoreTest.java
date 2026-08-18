package de.ingoschindler.kernel.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3ObjectStoreTest {

    @Mock
    S3Client s3;

    @InjectMocks
    S3ObjectStore store;

    @Test
    void putDelegatesToS3ClientWithCorrectBucketAndKey() {
        var data = new ByteArrayInputStream("content".getBytes());

        store.put("my-bucket", "attachments/file.bin", data, "application/octet-stream", 7);

        verify(s3).putObject(eq(PutObjectRequest.builder().bucket("my-bucket").key("attachments/file.bin")
                .contentType("application/octet-stream").build()), any(RequestBody.class));
    }

    @Test
    void deleteDelegatesToS3ClientWithCorrectBucketAndKey() {
        store.delete("my-bucket", "attachments/file.bin");

        verify(s3).deleteObject(DeleteObjectRequest.builder().bucket("my-bucket").key("attachments/file.bin").build());
    }
}
