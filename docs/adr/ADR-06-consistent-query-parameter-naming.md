# ADR-06: Consistent Query Parameter Naming

**Status:** Accepted
**Reversibility:** low once clients exist.

## Context

Query parameter names are chosen one endpoint at a time. Without a convention the
same concept arrives as `pageSize` here, `page_size` there, `limit` in the third
place, and every client has to special-case each one. The cost lands entirely on
callers, which is why it is easy for the server side not to notice.

## Decision

- **camelCase**, matching the JSON body style, so a client uses one naming
  convention throughout. Not `snake_case`, not `kebab-case`.
- **Pagination is always `page` and `pageSize`.** `page` is one-based and defaults
  to 1; `pageSize` defaults to 25 and is capped server-side at 100. Not
  `offset`/`limit`: a caller thinking in pages should not have to do the
  arithmetic, and the cap belongs on the server because an unbounded page size is
  a caller-supplied denial of service.
- **Booleans are positive.** `includeArchived=true`, never `excludeArchived=false`.
  A negated flag inverts in the reader's head every time it is read.
- **Filters are named after the field they filter**, so `status=ACTIVE` filters
  `status`. A parameter whose name does not match a field needs a comment.
- **Enum values are sent as the enum name**, uppercase, exactly as the API
  documents them. No case-insensitive parsing: accepting `active` and `ACTIVE`
  means two spellings in client code and logs.

## Rationale

One-based pagination is the only slightly contentious choice. Zero-based matches
array indices; one-based matches what a user interface displays and what a person
says out loud. Since the parameter is set by a client rendering a page number, it
matches the client.

The server-side cap is not politeness. Without it, `?pageSize=1000000` is a
one-request memory exhaustion.

## Consequences

- The cap lives in one place (the pagination request type), not per endpoint.
- A client that sends `pageSize=0` or a negative page gets the default rather than
  an error: coercing an obviously-nonsensical value is friendlier than a 400 and
  cannot be exploited, since the result is still bounded.

## Related

- ADR-05 — where query parameters belong at all
- ADR-13 — what the pagination envelope reports for an empty result
