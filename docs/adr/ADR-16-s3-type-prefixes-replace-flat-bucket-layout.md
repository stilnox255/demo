# ADR-16: Type Prefixes Instead of One Bucket per Kind

**Status:** Accepted
**Reversibility:** medium — moving existing objects requires a copy.

## Context

A new kind of stored object needs somewhere to live. The two options are a new
bucket or a new key prefix inside the existing one, and the bucket looks tidier.

A bucket is not free. It is an operational object: it needs a policy, possibly a
lifecycle rule, credentials that can reach it, a line in the provisioning script,
and a line in every environment that has to be created before the application
starts. Multiply that by every kind of file and bucket creation becomes a step in
the deployment runbook.

A flat bucket with no structure is the other failure: everything in the root, no
way to apply a lifecycle rule to one class of object, and no way to tell from a
key what it is.

## Decision

One bucket per environment. Object types separated by key prefix, and the prefixes
are constants in one place:

```java
public final class S3Prefixes {
    public static final String ATTACHMENTS = "attachments/";
}
```

A new kind of object gets a constant here. It does not get a bucket unless it
needs something a bucket provides and a prefix does not — a different retention
policy, a different access boundary, or different credentials.

Keys inside a prefix are generated, not caller-supplied: a random identifier plus
the original extension. The caller's filename is stored in the catalogue row, not
in the key. A filename in an object key invites both collisions and path
traversal.

## Rationale

The prefix carries everything a lifecycle rule or a listing needs, at no
operational cost. Bucket-per-type buys isolation that this application does not
need, and pays for it on every environment setup.

## Consequences

- The bucket-creation step is a single idempotent operation, which is why the
  provisioning container can simply be re-run.
- Cross-prefix operations (list everything a user owns) go through the catalogue
  table, not through the object store's listing API — which is what you want
  anyway, since listing an object store is slow and eventually consistent.

## Related

- ADR-15 — the port that owns the prefixes
- ADR-17 — where the original filename is kept instead
