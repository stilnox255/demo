package de.ingoschindler.kernel.storage;

import de.ingoschindler.demo.application.port.in.AttachFileToDemoItemCommand;
import de.ingoschindler.demo.application.port.in.AttachFileToDemoItemUseCase;
import de.ingoschindler.demo.application.port.in.CreateDemoItemCommand;
import de.ingoschindler.demo.application.port.in.CreateDemoItemUseCase;
import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.kernel.upload.UploadedFile;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * One {@code storage_ref} row per upload, even for identical bytes (ADR-18).
 *
 * <p>Deduplicating on content hash looks like free savings and is a correctness
 * trap: the second owner's delete takes the first owner's file with it, and the
 * bug surfaces as a broken download in an unrelated account. The hash is recorded
 * so a duplicate can be *recognised*, not so a row can be shared.</p>
 *
 * <p>S3 itself is mocked — what is under test is the row bookkeeping, and a real
 * object store would only make the test slower and flakier.</p>
 */
@QuarkusTest
class StorageRefPerUploadIT {

    @Inject
    CreateDemoItemUseCase createDemoItem;

    @Inject
    AttachFileToDemoItemUseCase attachFile;

    @InjectMock
    ObjectStoragePort objectStorage;

    @BeforeEach
    void stubStorage() {
        ObjectStorageStub.stubWrites(objectStorage);
    }

    @Test
    void sameOwnerSameBytesGetSeparateStorageRefs() {
        String ownerId = UUID.randomUUID().toString();
        byte[] content = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);

        UUID first = attachTo(itemOf(ownerId), ownerId, content);
        UUID second = attachTo(itemOf(ownerId), ownerId, content);

        assertNotNull(first);
        assertNotEquals(first, second, "identical bytes must still get their own storage_ref row");
    }

    @Test
    void differentOwnersSameBytesGetSeparateStorageRefs() {
        String ownerA = UUID.randomUUID().toString();
        String ownerB = UUID.randomUUID().toString();
        byte[] content = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);

        UUID refA = attachTo(itemOf(ownerA), ownerA, content);
        UUID refB = attachTo(itemOf(ownerB), ownerB, content);

        assertNotEquals(refA, refB, "one owner's delete must never reach another owner's blob");
    }

    private DemoItem itemOf(String ownerId) {
        return createDemoItem.create(new CreateDemoItemCommand("item", "", ownerId)).item();
    }

    private UUID attachTo(DemoItem item, String ownerId, byte[] content) {
        UploadedFile file = new UploadedFile("note.txt", "text/plain", content.length,
                () -> new ByteArrayInputStream(content));

        return attachFile.attach(new AttachFileToDemoItemCommand(item.id(), ownerId, file)).item().findAttachment()
                .orElseThrow().storageRefId();
    }
}
