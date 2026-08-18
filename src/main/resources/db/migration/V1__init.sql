-- Initial schema: the storage catalogue plus the demo aggregate.
--
-- One migration because this is a starting point, not a history. Every change
-- from here on is a new numbered file: Flyway migrations are append-only, and
-- editing one that has already run leaves every existing database behind
-- (`validate-on-migrate` will say so loudly, which is the point).

-- Catalogue of stored blobs, owned by kernel.storage (ADR-17). A normalized
-- table rather than columns copied onto every owner: the blob metadata is the
-- same everywhere, and an owning row only needs the reference.
CREATE TABLE storage_ref
(
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    bucket       VARCHAR(255) NOT NULL,
    prefix       VARCHAR(255) NOT NULL,
    key_name     VARCHAR(255) NOT NULL,
    content_type VARCHAR(255),
    size         BIGINT,
    -- Content hash of the stored bytes. Recorded for every row so a duplicate
    -- upload can be recognised; deliberately NOT unique, because one blob per
    -- upload is the rule here (ADR-18) — sharing a row between owners means the
    -- second owner's delete breaks the first owner's download.
    hash         VARCHAR(64)  NOT NULL,
    owner_id     VARCHAR(255) NOT NULL,
    CONSTRAINT pk_storage_ref PRIMARY KEY (id)
);

CREATE INDEX idx_storage_ref_owner_hash ON storage_ref (owner_id, hash);

CREATE TABLE demo_item
(
    id                        UUID          NOT NULL DEFAULT gen_random_uuid(),
    name                      VARCHAR(120)  NOT NULL,
    description               VARCHAR(2000) NOT NULL DEFAULT '',
    -- Stored as text, not as an integer: an ordinal column silently remaps every
    -- row the day someone reorders the enum in Java.
    status                    VARCHAR(32)   NOT NULL,
    owner_id                  VARCHAR(255)  NOT NULL,
    -- Attachment metadata is denormalized from storage_ref on purpose (ADR-03):
    -- the download path would otherwise join into a table this BC does not own.
    attachment_storage_ref_id UUID REFERENCES storage_ref (id),
    attachment_file_name      VARCHAR(255),
    attachment_content_type   VARCHAR(255),
    attachment_size           BIGINT,
    created_at                TIMESTAMP     NOT NULL,
    -- Optimistic-lock token. NOT NULL with a default so an INSERT that omits it
    -- cannot start life at NULL and break the first UPDATE.
    version                   BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_demo_item PRIMARY KEY (id)
);

-- Covers the list and summary queries, which are always owner-scoped and always
-- newest-first. The sort column belongs in the index; without it every list
-- request sorts the owner's rows again.
CREATE INDEX idx_demo_item_owner_created ON demo_item (owner_id, created_at DESC);

-- Partial index for the archive job: it only ever looks at drafts, so indexing
-- the other states would be paid for on every write and read by nobody.
CREATE INDEX idx_demo_item_stale_drafts ON demo_item (created_at) WHERE status = 'DRAFT';
