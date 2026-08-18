# ADR-14: REST Exception Mapper Logging Convention

**Status:** Accepted
**Reversibility:** high — log levels, per mapper.

## Context

Every exception mapper logs. Without a convention each one picks a level by feel,
and the level chosen is almost always too high — a 404 at `WARN`, a validation
failure at `ERROR`, because at the moment of writing the mapper the failure feels
important.

The consequence is not noise for its own sake. An error log that fills with normal
client traffic is an error log nobody reads, and the one entry that mattered was
in there somewhere during the incident.

## Decision

One log record per mapped exception, at the level its severity bucket prescribes.

| Bucket | Meaning | Level | Throwable attached | Typical status |
|---|---|---|---|---|
| Client outcome | The caller asked for something that is not there, or sent something invalid. Nothing is wrong with the service. | `DEBUG` | no | 400, 404 |
| State conflict | The request was well-formed but lost a race, or conflicts with current state. The caller can retry. | `INFO` | no | 409, 412 |
| Dependency failure | Something the service depends on is unreachable, unconfigured or misbehaving. An operator has to see it. | `WARN` | yes | 502, 503 |
| Unexpected | A bug. Nobody predicted this path. | `ERROR` | yes | 500 |

The throwable is attached only in the last two buckets. A stack trace for a 404
costs log volume and tells nobody anything; a stack trace for a 500 is the entire
diagnostic value of the record.

Message format is structured, not prose: `event_name key=value key=value`, so a
log query can aggregate it. Every message carries the status and the operation;
dependency failures also carry which dependency.

**A 500's response body never carries the cause** (ADR-08). The detail belongs in
the log, where the caller cannot read it.

## Rationale

The convention is only worth having if it is checked. Left to review, the next
mapper logs its 404 at `WARN`, and a month later the error log is mostly client
typos. So there is a test that asserts the level and the presence of the throwable
per bucket — see `ExceptionMapperLoggingTest`.

## Consequences

- A conflict response (optimistic-lock failure) logs at `INFO`, which is a change
  from the intuitive `ERROR`. It is a client-caused, client-recoverable outcome.
- Adding a mapper means picking a bucket, and the test enforces it.

## Related

- ADR-08 — the response bodies
- ADR-23 — the correlation ids that make a record findable
