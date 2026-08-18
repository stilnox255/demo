package de.ingoschindler.demo.domain;

import java.util.UUID;

/**
 * Thrown when no item with the given id is visible to the caller.
 *
 * <p>Deliberately does not distinguish "does not exist" from "belongs to
 * someone else": both answers are 404, because telling an attacker that an id
 * exists but is not theirs is the enumeration half of an IDOR.</p>
 */
public class DemoItemNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DemoItemNotFoundException(UUID id) {
        super("No demo item with id " + id);
    }
}
