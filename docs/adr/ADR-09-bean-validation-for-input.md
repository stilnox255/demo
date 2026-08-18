# ADR-09: Bean Validation for Input

**Status:** Accepted
**Reversibility:** high — annotations, per DTO.

## Context

Request validation has to happen somewhere. Hand-written checks in the resource
method are the default: a chain of `if` statements returning 400. They work, and
they have three problems that only show up over time.

They stop at the first failure, so a client with three bad fields needs three
round trips. They are invisible to the generated API documentation. And they
drift from the domain's own rules, because nothing connects the two.

## Decision

Bean Validation on the request DTO, checked by the framework before the resource
method runs. Constraints and documentation on the same field:

```java
public record DemoItemRequest(
        @NotBlank @Size(max = DemoItem.NAME_MAX_LENGTH)
        @Schema(required = true, examples = "My first item") String name,
        ...) { }
```

The bounds reference the domain's own constants rather than repeating the number,
so the two cannot drift apart.

The violation is turned into a problem-details body with one entry per failed
field (ADR-08), so one response tells a client everything that is wrong.

**The domain still validates.** The compact constructor of the aggregate rejects
the same blank name. That is not duplication: the DTO's job is to reject a bad
*request* with a helpful message, the domain's job is to make an invalid *state*
unrepresentable. The domain check is what protects the use case that is driven by
a timer or a message rather than by HTTP.

## Rationale

The framework does the traversal, the collection of violations and the mapping.
What is left to write is the constraint itself, which is also the documentation.

## Consequences

- Cross-field rules that annotations express awkwardly (`expectedVersion` is
  required on update but not on create) stay as an explicit check in the resource.
  Bending a custom class-level validator around a conditional requirement is more
  code than the `if`.
- Validation messages are user-visible. They are checked in the integration
  tests, which is what stops a framework upgrade from silently rewording them.

## Related

- ADR-08 — the response shape a violation produces
- ADR-10 — why the request is a typed record in the first place
