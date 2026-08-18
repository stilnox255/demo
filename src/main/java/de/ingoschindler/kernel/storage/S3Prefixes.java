package de.ingoschindler.kernel.storage;

/**
 * Canonical S3 key prefixes. One bucket per environment, object *types*
 * separated by key prefix rather than by bucket — see ADR-16. Bucket creation
 * is an operational act (policies, lifecycle rules, credentials); a prefix is
 * free, so a new kind of object gets a constant here and not a new bucket.
 *
 * <p>Lives in the storage package rather than in a BC because the prefixes are
 * part of the storage contract shared by every caller, and they carry no
 * infrastructure dependency of their own.</p>
 */
public final class S3Prefixes {

    /** User-uploaded files attached to an aggregate. */
    public static final String ATTACHMENTS = "attachments/";

    private S3Prefixes() {
    }
}
