# ADR-10: Typed DTOs Instead of Untyped Maps

**Status:** Accepted
**Reversibility:** high per endpoint.

## Context

A `Map<String, Object>` is the fastest way to return JSON. No class to write, no
class to update when a field is added, and the serialiser handles it.

It also compiles against any typo, documents nothing in the generated API
specification, turns every field rename into a runtime surprise in a client, and
gives a reader no way to find out what an endpoint returns except by running it.
The same applies to a generic `Object` field, and to a `Map` nested inside an
otherwise typed response.

## Decision

Every request and response body is a record in `adapter.in.rest.dto`, with schema
annotations. No `Map<String, Object>`, no `Object`-typed field, no untyped nested
structure.

```java
@Schema(name = "DemoItem", description = "A demo item")
public record DemoItemResponse(UUID id, String name, DemoItemStatus status,
        Instant createdAt, long version, Attachment attachment) {

    @Schema(name = "DemoItemAttachment")
    public record Attachment(String fileName, String contentType, long size, String downloadUrl) { }
}
```

Nested structures are nested records. An enum is an enum, not a string.

The mapping from domain type to DTO lives in one place per BC
(`{Bc}Mapper`), so a field added to the response has exactly one place to be
filled in.

Cached values follow the same rule and one more: the projection that goes into a
cache is a wire format with a deployed-version skew problem attached, so it stays
as small as it can be and its shape changes are versioned (ADR-41).

## Rationale

The record costs five lines and pays back as generated documentation, as
compile-time feedback on a rename, and as something a reader can navigate to.

## Consequences

- A response shape change is a compile error at the mapper, which is where you
  want to be told.
- The generated OpenAPI document is usable rather than a list of `object`.
- Records are immutable, so a response cannot be half-built.

## Related

- ADR-09 — the constraints these records carry
- ADR-11 — what turns the annotations into documentation
