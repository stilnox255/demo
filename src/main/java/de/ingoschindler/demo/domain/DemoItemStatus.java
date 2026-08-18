package de.ingoschindler.demo.domain;

/**
 * Lifecycle of a {@link DemoItem}.
 *
 * <p>An enum and not a boolean pair: the states are mutually exclusive and the
 * set will grow before it shrinks. Persisted by name (see the JPA entity), never
 * by ordinal — reordering the constants must not rewrite history.</p>
 */
public enum DemoItemStatus {

    /** Created, not yet published. The only state the archive job may touch. */
    DRAFT,

    /** In use. */
    ACTIVE,

    /** Retired. Terminal — nothing transitions out of it. */
    ARCHIVED
}
