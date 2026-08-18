# ADR-15: Shared Object-Store Abstraction

**Status:** Accepted
**Reversibility:** medium — one port, several callers.

## Context

Storing a file has two halves: the bytes go to an object store, and a record of
them goes to the database so the application can find them again, know their
content type, and delete them.

Left to grow organically, each feature that stores a file writes its own version
of both halves. The observable end state is two nearly-identical facades over the
same object store, each with slightly different key conventions and its own
metadata columns — and no single place to add a timeout, a metric, or a retry.

## Decision

One outbound port, `ObjectStoragePort`, for both halves. Every caller that needs a
blob goes through it.

Writing is split, deliberately:

```java
String putBlob(bucket, prefix, fileName, data, contentType, size);   // bytes only
UUID   registerBlob(bucket, prefix, key, contentType, size, hash, ownerId); // row only
UUID   storeAndRegister(bucket, prefix, uploadedFile, hash, ownerId);       // both
```

The split exists so a caller can push the bytes **outside** a database
transaction and record the reference inside one (ADR-04). An upload must never
hold a JDBC connection open for the duration of a network transfer. The one-shot
method stays for callers that genuinely want both side effects at once.

Blobs are identified to the application layer by the opaque catalogue row id, not
by bucket and key. The application never sees the persistence mapping, nor the key
layout behind it.

Reading and deleting take that id. There is no "delete the blob but keep the row"
operation: a row without a backing blob is never useful.

## Rationale

The port is where the cross-cutting concerns can exist at all — transport
timeouts, fault tolerance, and the decision that a retry is safe on a download but
not on an upload (ADR-20). Two facades means picking one of them to protect.

Returning an opaque id rather than a key is what keeps the key layout changeable
(ADR-16) without touching a caller.

## Consequences

- The port has a slightly awkward six-argument write method. The alternative, a
  parameter object, was rejected as a wrapper whose only job is to be unpacked
  again one line later.
- The catalogue table is owned by the kernel, and other components reference its
  id as a plain column (ADR-03).

## Related

- ADR-16 — the key layout inside the bucket
- ADR-17 — the catalogue table
- ADR-18 — one row per upload
