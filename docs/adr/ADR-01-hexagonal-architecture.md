# ADR-01: Hexagonal Architecture for the Backend

**Status:** Accepted
**Reversibility:** low — the layout is load-bearing for every other decision here.

## Context

A server application needs a home for three different kinds of code: the rules
that define what the software is for, the wiring that lets it be called, and the
adapters that let it call something else. Left unseparated they mix, and the mix
is stable — nobody ever un-mixes it later.

The usual first layout is Boundary-Control-Entity: one package for the HTTP
surface, one for services, one for entities. It works while the domain is small.
Past that point the entity package accretes four unrelated roles:

1. Pure domain types.
2. JPA mappings — every aggregate extends a Panache entity, so the "domain" type
   *is* the persistence mapping.
3. REST DTOs with schema and JSON annotations, often as `toDto()` methods on the
   JPA-bound class.
4. Framework-bound request records carrying multipart or form annotations.

The leakage that follows is structural, not stylistic: resources call static
finders on domain types, a persistence cursor type appears as a return type at
the HTTP boundary, a file-upload type from the web framework appears in the
signature of an internal port, and a domain method takes an injected token
signer as an argument. Each one is individually defensible and collectively
means the domain cannot be instantiated without the runtime.

## Decision

Hexagonal Architecture (Ports and Adapters), one class per use case. Per
business component:

```
{root}.{bc}
├── domain                              (pure POJOs — no JPA, no JAX-RS, no framework)
│   ├── {Aggregate}.java
│   ├── {ValueObject}.java
│   ├── {Enum}.java
│   ├── {DomainException}.java
│   ├── event/{DomainEvent}.java
│   └── service/{DomainService}.java
├── application
│   ├── usecase
│   │   └── {Verb}{Noun}Service.java     (implements {Verb}{Noun}UseCase)
│   └── port
│       ├── in/{Verb}{Noun}UseCase.java  (published API — every use case, always)
│       ├── in/{Verb}{Noun}Command.java
│       ├── in/{Verb}{Noun}Result.java
│       ├── out/{Aggregate}Repository.java
│       └── out/{Gateway}Port.java
└── adapter
    ├── in
    │   ├── rest/{Bc}Resource.java
    │   ├── rest/dto/                   (request/response DTOs + mapper)
    │   ├── scheduled/                  (timer-driven entry points)
    │   └── messaging/                  (event handlers)
    └── out
        ├── persistence
        │   ├── {Aggregate}JpaEntity.java
        │   ├── Jpa{Aggregate}Repository.java
        │   └── {Aggregate}PersistenceMapper.java
        └── {externalSystem}/
```

`*UseCase` for the published interface, `*Service` for the implementing class.
That is the common convention for this pattern and it earns "Service" back as a
suffix for exactly one thing: a class implementing one in-port. It does not
reopen the door to a service class that does everything.

Not every application class fits the single-action `Command in, Result out`
shape. A multi-method read facade has no single use case being executed, and
forcing the naming onto it is misleading. Those get a parallel pair instead:

- `{Noun}QueryPort` — the in-port interface
- `{Noun}Query` — the implementing class, no further suffix

Command-Query Separation at the class level, not event-sourced read models.

Cross-BC types live in `kernel`. One BC may depend on another only through
`{otherBc}.application.port.in`, never on its `adapter.*`,
`application.usecase.*` or `domain.*`. Every use case publishes an in-port
whether or not another component calls it today, and inbound adapters — including
the BC's own — depend on the interface, never the concrete class.

## Rationale

- **A pure domain is testable with plain values.** A domain type bound to a
  persistence superclass needs the runtime to instantiate, so its rules get
  covered by slow integration tests or by nothing.
- **Repository ports kill the static finder.** Resources stop reaching into
  `Entity.find(...)`, which is what allows a hand-written fake instead of an
  in-memory database.
- **Framework types stopping at the adapter** means the same use case can be
  driven by a timer, a message consumer or a test without an HTTP shim.
- **One class per use case makes the application API discoverable.** A commit
  can name the use cases it introduces.

## Consequences

- More files per aggregate: a domain type, a JPA entity, a repository port, an
  adapter and a mapper. The offset is that most of the interesting tests need no
  test doubles at all.
- Mappers are hand-written. A generator needs the entity's accessors to match the
  record's, and `name()` is not `getName()`.
- Extending a persistence entity superclass is forbidden outside
  `adapter.out.persistence`, enforced by ArchUnit.
- DTOs leave the domain. Domain types carry no schema or serialisation
  annotations.
- The rules are fitness functions, not review items — see
  `src/test/java/de/ingoschindler/architecture/HexagonalArchitectureTest.java`.
  A convention that is only written down erodes one pragmatic import at a time.

## Related

- ADR-02 — what belongs in `kernel` and what does not
- ADR-03 — how two BCs share data without sharing types
- ADR-04 — where transaction boundaries live
