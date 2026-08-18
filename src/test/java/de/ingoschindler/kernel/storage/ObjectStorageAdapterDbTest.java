package de.ingoschindler.kernel.storage;

import de.ingoschindler.kernel.storage.adapter.out.persistence.StorageRefJpaEntity;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ObjectStorageAdapterDbTest {

    @Inject
    ObjectStoragePort objectStorage;

    @InjectMock
    S3ObjectStore s3;

    /** Write a blob the way every production caller does: put, then register. */
    private UUID store(String fileName, String contentType, byte[] content, String hash) {
        var key = objectStorage.putBlob("files", "attachments/", fileName, new ByteArrayInputStream(content),
                contentType, content.length);
        return objectStorage.registerBlob("files", "attachments/", key, contentType, content.length, hash, "user-1");
    }

    @Test
    @Transactional
    void uploadCreatesStorageRef() {
        var content = "attachment-data".getBytes();
        when(s3.put(any(), any(), any(), any(), anyLong())).thenReturn("attachments/test-key.txt");

        var ref = store("notes.txt", "text/plain", content, "abc123hash");

        assertNotNull(ref);
        var loaded = StorageRefJpaEntity.<StorageRefJpaEntity>findById(ref);
        assertNotNull(loaded);
        assertEquals("files", loaded.bucket);
        assertEquals("attachments/", loaded.prefix);
        assertNotNull(loaded.key);
        assertEquals("text/plain", loaded.contentType);
        assertEquals((long) content.length, loaded.size);
        assertEquals("abc123hash", loaded.hash);
        assertEquals("user-1", loaded.ownerId);
    }

    @Test
    @Transactional
    void downloadResolvesStorageRef() throws IOException {
        var content = "round-trip-bytes".getBytes();
        when(s3.put(any(), any(), any(), any(), anyLong())).thenReturn("attachments/rt-key.txt");
        when(s3.get(any(), any())).thenReturn(
                new ResponseInputStream<>(GetObjectResponse.builder().build(), new ByteArrayInputStream(content)));

        var ref = store("roundtrip.txt", "text/plain", content, "rt-hash");

        var downloaded = objectStorage.download(ref).readAllBytes();
        assertArrayEquals(content, downloaded);
    }

    /**
     * The generated key keeps the extension but not the caller's filename: a
     * caller-supplied name in an object key invites both collisions and traversal.
     */
    @Test
    @Transactional
    void generatedKeyIsUniqueAndKeepsTheExtension() {
        var content = "second".getBytes();
        when(s3.put(any(), any(), any(), any(), anyLong())).thenReturn("attachments/second-key.txt");

        var first = store("second.txt", "text/plain", content, "second-hash");
        var second = store("second.txt", "text/plain", content, "second-hash");

        var firstKey = StorageRefJpaEntity.<StorageRefJpaEntity>findById(first).key;
        var secondKey = StorageRefJpaEntity.<StorageRefJpaEntity>findById(second).key;
        assertTrue(firstKey.endsWith(".txt"));
        assertFalse(firstKey.contains("second.txt"));
        assertNotEquals(firstKey, secondKey);
    }

    @Test
    @Transactional
    void deleteRemovesBlobAndStorageRefRow() {
        var content = "to-be-deleted".getBytes();
        when(s3.put(any(), any(), any(), any(), anyLong())).thenReturn("attachments/del-key.txt");

        var ref = store("gone.txt", "text/plain", content, "del-hash");

        objectStorage.delete(ref);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3).put(any(), keyCaptor.capture(), any(), any(), anyLong());
        verify(s3).delete("files", keyCaptor.getValue());
        assertNull(StorageRefJpaEntity.findById(ref));
    }
}
