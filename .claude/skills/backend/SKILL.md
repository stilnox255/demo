---
name: microprofile-server
description: Architecture and coding rules for long-running Java MicroProfile / Jakarta EE server applications — Hexagonal Architecture (Ports & Adapters), one class per use case, business components (BC), JAX-RS adapters, CDI, JSON-P, testing (unit/integration/system), and gradle project structure. Use when creating, generating, scaffolding, writing, or reviewing code, resources, domain types, use cases, or business components in MicroProfile server projects. Not for serverless deployments.
---

## Related Skills
- consult the `quarkus` skill for framework-specific guidance: REST, CDI/ArC, Hibernate ORM, Panache, configuration, OpenAPI, messaging, security (OIDC/JWT), observability, native/packaging, and tooling
- consult the `quarkus-panache-smells` skill when writing or reviewing PanacheRepository code: N+1 queries, missing projections, pagination, eager fetching, unclosed streams
- apply both alongside the rules below; on conflict, the project rules in this skill take precedence

## Java Version & Syntax
- use Java 25 with modern syntax (var, pattern matching, records, text blocks)
- prefer dependencies in this order: Java SE, MicroProfile, Jakarta EE
- never add a dependency that is added transitively by another dependency
- use Java SE APIs over writing custom code
- prefer the most specific Java SE type for the domain
- prefer unchecked over checked exceptions; never throw generic exceptions like java.lang.Exception
- throw RuntimeException subclasses, not directly; inherit from WebApplicationException in JAX-RS projects
- consider using Java records instead of classes with final fields
- prefer factory methods in records over passing null in constructors
- always use OffsetDateTime instead of LocalDateTime for time zone awareness unless explicitly required

## Logging
- use org.jboss.logging.Logger instead of System.out statements
- never use java.util.logging.Logger or java.lang.System.Logger
- Logger fields must be named LOGGER (uppercase) and marked as static final, created via `Logger.getLogger(SomeClass.class)`

## Hexagonal Architecture (Ports & Adapters)
- structure code using Hexagonal Architecture with one class per use case in the application layer
- top level package reflects the application responsibility or name; business components (BC) are children of the top level package, named after their responsibilities
- per-BC package layout: `[ORG].[PROJECT].[BC].{domain | application | adapter.in | adapter.out}`
- cross-cutting primitives (page records, upload records, error types, shared value objects) live in `[ORG].[PROJECT].shared.kernel` — never inside a BC
- one BC may depend on another only through `{otherBc}.application.port.in` (published `*UseCase`/`*QueryPort` interface) — never on `{otherBc}.adapter.*`, `{otherBc}.application.usecase.*`, or `{otherBc}.domain.*` directly; every use case publishes a port.in interface, no exceptions
- do not explain the hexagonal pattern in documentation; link to the project ADR instead

## ArchUnit-Enforced Rules
Rules below are compiler/test-enforced by `HexagonalArchitectureTest` (`src/test/java/de/ingoschindler/architecture/`) — violating them fails the build, not just review. Naming-convention rules elsewhere in this document (UseCase/Service/Repository/Port/Command/Result suffixes) are convention only, not machine-checked.

- core (`domain`/`application`) never depends on `jakarta.ws.rs`, RESTEasy, `jakarta.persistence`, Hibernate, Panache, or MP OpenAPI (`coreIsFrameworkFree`)
- an adapter never depends on another BC's `domain` (`adaptersDoNotDependOnAnotherBcsDomain`); a BC's `domain` never depends on another BC's `domain` (`busBccDomainsDoNotDependOnOtherBccDomains`)
- `adapter.in` never depends on `application.usecase` — inbound adapters call the `*UseCase`/`*QueryPort` interface, never the concrete `*Service`/`*Query` class, even within the same BC (`inboundAdaptersOnlyAccessPortsNotUseCases`)
- `adapter.in` never depends on `adapter.out.persistence` (`inboundAdaptersDoNotDependOnPersistenceAdapter`)
- `@Entity` classes live only in `adapter.out.persistence` (`jpaEntitiesLiveOnlyInPersistenceAdapter`); so does anything extending `PanacheEntity`/`PanacheEntityBase` (`panacheEntityBaseLivesOnlyInPersistenceAdapter`) or depending on `PanacheQuery` (`panacheQueryStaysInPersistenceAndSanctionedBridges`)
- `application` never depends on RESTEasy's multipart `FileUpload` (`uploadedFileNotJaxRsFileUploadInApplication`)
- `adapter.out` never depends on another BC's `adapter.out` (`adapterOutDoesNotDependOnAnotherBcsAdapterOut`)
- no `boundary`/`control`/`entity` packages remain anywhere (`noLegacyBceLayersRemain`)

## Domain Layer (`{bc}.domain`)
- POJOs carrying business logic (Rich Domain Model, not anemic/Active Record): entities carry business calculations, state transitions, and invariant checks; "POJO" means free of framework coupling, not free of behavior
- no imports from `jakarta.persistence`, `jakarta.ws.rs`, `io.quarkus.*`, `org.jboss.resteasy.*`, MapStruct, or any adapter package (`coreIsFrameworkFree`)
- no `@Inject`, no `@Entity`, no `@Schema`, no `@JsonbProperty`, no `@RestForm`
- external data required by a domain method is passed as a method parameter; never inject infrastructure into a domain class
- model value objects as records; model finite sets as enums
- never `extends PanacheEntity` / `PanacheEntityBase` — that's an adapter-layer concern (`panacheEntityBaseLivesOnlyInPersistenceAdapter`)

## Application Layer (`{bc}.application`)
- every use case is a pair: a `{Verb}{Noun}UseCase` (single-action) or `{Noun}QueryPort` (multi-method read facade) interface under `{bc}.application.port.in`, implemented by exactly one `{Verb}{Noun}Service` / `{Noun}Query` class under `{bc}.application.usecase` — always, not only when another BC needs it; inbound adapters (including this BC's own) may only depend on the port.in interface, never the concrete class (`inboundAdaptersOnlyAccessPortsNotUseCases`)
- input as a `Command` record (`{Verb}{Noun}Command`) when the use case takes more than one or two primitives, or the parameter set is likely to grow; plain method parameters (e.g. `execute(UUID id)`) are fine for simple single/two-argument lookups — don't wrap a trivial parameter list in a `Command` for its own sake
- `Command`/`Result` records live in `{bc}.application.port.in`, next to the interface they belong to — not in `application.usecase`
- output as a `Result` record (`{Verb}{Noun}Result`) or a domain type directly when no shape transformation is needed
- use case method signature: `Result execute(Command cmd)` (or a domain-type return, or a plain-parameter signature per above)
- use cases depend on out-ports (`{bc}.application.port.out.*`) and domain types; never on adapter classes, JAX-RS, JPA, or REST DTOs
- `@Transactional` is allowed here when the use case writes through a repository
- `@ApplicationScoped` beans for use cases — the only CDI scope used in this codebase; these beans are stateless (no mutable instance fields), so the client-proxy singleton introduces no contention; no field injection — declare ports as fields and let CDI inject

## Adapter Layer (`{bc}.adapter`)
- `adapter.in.rest` — JAX-RS resources; resources are thin and delegate to a use case
- `adapter.in.rest.dto` — request/response records, optional `*Mapper` to/from domain types; mapping between the adapter's external representation and the domain types spoken by the port belongs here, not in `application.port.*` (which is interfaces only, no implementation)
- `adapter.in.scheduled` — `@Scheduled` jobs only; thin, delegate to a use case
- `adapter.in.messaging` — `@ConsumeEvent` handlers only; thin, delegate to a use case
- `adapter.out.persistence` — JPA entity (`{Aggregate}JpaEntity`), Panache repository implementation (`Jpa{Aggregate}Repository implements {Aggregate}Repository`), persistence mapper
- `adapter.out.{external}` — external-system adapters (object storage, HTTP APIs, message brokers, identity providers) implementing an out-port
- `extends PanacheEntity` / `extends PanacheEntityBase` is forbidden outside `adapter.out.persistence` (`panacheEntityBaseLivesOnlyInPersistenceAdapter`)

## Package Naming
- create application level package with name derived from gradle project or context
- name packages after their domain responsibilities
- create package-info.java for top level packages with JavaDoc documenting design decisions and responsibilities (not contents)
- document only domain-specific packages with package-info.java where the purpose is not self-evident

## Inbound Adapters (`adapter.in.*`)
- JAX-RS resources contain no business logic; they translate HTTP to a use-case `Command`, invoke `useCase.execute(cmd)`, translate the result back to HTTP
- health checks live here (`adapter.in.health` or alongside the resource)
- `@ConsumeEvent` handlers stay thin: receive event, build a `Command`, delegate to a use case, return
- scheduled jobs (`@Scheduled`) stay thin: build a `Command`, delegate to a use case
- prefer ApplicationScoped beans; no `@Transactional` on inbound adapters — transactional boundaries belong to the use case

## Out-Ports (`{bc}.application.port.out.*`)
- one out-port interface per external concern (`{Aggregate}Repository`, `{External}Gateway`, `{External}Client`)
- methods speak in domain types and primitives; never in JPA, JAX-RS, or adapter-specific types
- a port may return a domain type, a `shared.kernel.Page<T>` of domain types, a domain `Optional`, or a small port-local record for infra-shaped results (e.g. a detection-result record) that isn't itself a domain aggregate
- returning domain types is intentional, not a leak: the adapter implementing the port is responsible for translating the infra-specific representation (JPA entity, HTTP response, vendor SDK object) into the domain type before it crosses the port boundary
- never expose `PanacheQuery<>`, `Stream<JpaEntity>`, `Response`, `FileUpload`, or vendor SDK types through a port (`panacheQueryStaysInPersistenceAndSanctionedBridges`)

## Persistence Adapter (`{bc}.adapter.out.persistence`)
- the JPA-bound class is `{Aggregate}JpaEntity` and lives only in this package (`jpaEntitiesLiveOnlyInPersistenceAdapter`)
- `Jpa{Aggregate}Repository implements {bc}.application.port.out.{Aggregate}Repository` is the only place that talks to Panache / `EntityManager`
- a `{Aggregate}PersistenceMapper` translates between the JPA entity and the domain POJO — hand-rolled today across this codebase (MapStruct is a declared but currently unused dependency; do not introduce it without discussion)
- the JPA entity's shape does not need to mirror the domain object 1:1 — that's what the ORM is for: map between the storage-efficient shape and the domain-efficient shape; denormalize a cross-BC reference as scalar snapshot columns (e.g. `{ref}Id`, `{ref}Name`) instead of a JPA association across BC boundaries, since `adapter.out` may never depend on another BC's `adapter.out` (`adapterOutDoesNotDependOnAnotherBcsAdapterOut`)
- JPA `@OneToMany` / `@ManyToOne` graphs within the same BC's aggregate are an implementation detail of the persistence adapter; the domain models its aggregate relationships in its own terms
- do not return JPA entities from repository methods; return domain types

## Class Naming Conventions
- name classes after their responsibilities
- avoid meaningless suffixes: *Impl, *Manager, *Creator
- class names must not end with "Control" (legacy BCE vocabulary)
- use "UseCase" suffix only for the port.in interface of a single-action use case (`UploadModelUseCase`); use "Service" suffix only for the class implementing it (`UploadModelService`) — "Service" is not a general-purpose suffix, it is reserved for this one role
- multi-method read facades (several finder methods, no `Command`/`Result` envelope) are not a `*UseCase` — use "QueryPort" for the port.in interface (`AnnotationExportQueryPort`) and "Query" (no further suffix) for the implementing class (`AnnotationExportQuery`)
- use "Command" / "Result" suffixes for use-case input/output records
- use "Repository" suffix for out-port repositories; persistence-adapter implementations are named `Jpa{Aggregate}Repository`
- use "Port" suffix for non-repository out-ports (`ObjectStoragePort`, `DownloadTokenPort`)
- only use "Resource" suffix for JAX-RS classes
- only use "Factory" suffix for actual GoF Factory pattern
- only use "Builder" suffix for classes with typical builder structure (method chaining)
- JPA-bound classes carry the "JpaEntity" suffix (`ModelJpaEntity`) and live only in `adapter.out.persistence`

## Visibility & Modifiers
- avoid private visibility; prefer package-private (default) visibility
- avoid "private static" methods; prefer default visibility
- do not use final for fields (exception: static final for LOGGER)
- do not use constructor injection

## Interfaces & Classes
- only use interfaces with multiple implementations or for strategy pattern — **exception: hexagonal port interfaces** (`*UseCase`/`*QueryPort`/`*Repository`/`*Port` under `application.port.*`, or a shared-kernel port like `ObjectStoragePort`) are single-impl by design; that's the point of Ports & Adapters, and ArchUnit enforces the split (`inboundAdaptersOnlyAccessPortsNotUseCases`). MicroProfile Rest Client interfaces (`@RegisterRestClient`) are exempt too — there's no hand-written implementation, the proxy is generated at runtime. This rule targets accidental interface+Impl boilerplate elsewhere, not these patterns
- do not create interfaces with abstract methods implemented by a single class; use classes directly (same port-pattern exception applies)
- create multiple classes only if it decreases complexity and increases readability

## Method Naming & Design
- avoid "getter" methods starting with "get"; prefer record convention (e.g., configuration() not getConfiguration())
- keep methods short and testable
- create well-named methods for coarse-grained, cohesive, self-contained logic
- if a lambda requires multiple statements or braces {}, extract it into a well-named helper method
- do not create multiline lambda expressions; use method references instead
- prefer extracting inline predicates into explaining methods and use method references (e.g., `.filter(this::isSkillFile)` over `.filter(p -> p.endsWith("SKILL.md"))`)
- extract repeated calculations or string concatenations into helper methods (DRY); apply **Rule of Three** — refactor on the third occurrence, not the second (avoid premature abstraction)
- do not create empty delegates which just call methods without added value

## Stream & Collections
- prefer java.util.stream.Stream API over for loops
- avoid forEach; prefer Stream methods
- prefer Stream.of to Arrays.stream
- prefer toList() to .collect(Collectors.toList())
- prefer List.of over String[] or new ArrayList<>()
- avoid creating unnecessary intermediate collections when streaming arrays
- prefer variable declaration over lengthy method chaining

## Code Style
- prefer multiple simpler lines to one more complex line
- prefer multiline Strings (text blocks) over String concatenations
- prefer imports over fully qualified class names
- use "this" to reference instance fields
- remove unused imports
- extract variables to eliminate duplication
- prefer enums over plain Strings for finite, well-defined values
- reuse enum constants as values if possible; enum constants do not have to follow naming conventions
- prefer try-with-resources over explicitly closing resources

## Simplicity Principles
- keep the design KISS and YAGNI
- always implement the simplest possible solution
- write simple code first; ask before implementing enhancements or optional features
- never over-engineer; ask about adding optional features or extension points
- create new components with minimal business logic and essential fields only

## Exceptions
- create custom exceptions only if it significantly improves robustness or maintainability
- domain exceptions extend `RuntimeException`, never `WebApplicationException` — `WebApplicationException` is a `jakarta.ws.rs` type, banned from `domain` by `coreIsFrameworkFree`; a dedicated `{Name}ExceptionMapper` in `adapter.in.rest.exception` translates each domain exception to a `Response` — see REST Exception Mapper Logging below for the full convention
- do not re-throw exceptions with "throw e" without adding value

## REST Exception Mapper Logging
Every JAX-RS `ExceptionMapper` emits exactly one log line per request handled. Severity is chosen by the operational nature of the failure, not by the HTTP status — see [ADR-14](../../../docs/adr/ADR-14-rest-exception-mapper-logging-convention.md) for the rationale and the full per-mapper bucket assignment.

| Bucket | When to use | Typical HTTP statuses |
|---|---|---|
| **DEBUG** | Pure client errors — malformed request, missing resource, static-input contract violation (`*NotFound`, `ConstraintViolation`, malformed JSON, unknown enums). Frequent, not actionable for operators. | 400, 404 |
| **INFO**  | State-conflict errors — request well-formed but system state legitimately rejected it (active model cannot be deleted, analysis already running, illegal status transition). | 400, 409, 422 |
| **WARN**  | Operational / dependency-state failures — something the service depends on is unhealthy, missing, or unconfigured (a dependency is unreachable, unconfigured, or has no capacity). Operator must see these. | 502, 503 |
| **ERROR** | Unhandled exceptions and infrastructure faults — already covered by `GlobalExceptionMapper` and `DatabaseExceptionMapper`. | 500 |

**Stacktrace policy:** WARN and ERROR mappers pass the throwable as the last logger argument (stacktrace logged). DEBUG and INFO mappers log the message + key fields only — no stacktrace; the exception class name in the message is sufficient context for expected control-flow failures.

**Format:** one line, message + `key=value` pairs. Key fields are the exception's own identifiers (resource id, role, dataset id) — never the full request body, never secrets.

**Pattern** (one `LOGGER` field per mapper class, one `LOGGER.log(...)` call before building the `Response`):

```java
@Provider
public class SomeExceptionMapper implements ExceptionMapper<SomeException> {

    static final Logger LOGGER = Logger.getLogger(SomeExceptionMapper.class);

    @Override
    public Response toResponse(SomeException exception) {
        LOGGER.infof("SomeFailure id=%s", exception.getId());
        return Response.status(...).type("application/problem+json").entity(...).build();
    }
}
```

When introducing a new mapper, assign it to the right bucket via ADR-14's severity buckets and follow the policy above.

## JavaDoc
- do not write obvious JavaDoc comments that rephrase code
- document the intentions and the "why", not implementation details
- either describe the "why" or do not comment at all
- follow links in JavaDoc to external specifications and use them for code generation
- use popular, also funny, technical terms from the Java SE, MicroProfile and Jakarta EE ecosystems as examples in unit tests and javadoc

## README Guidelines
- write brief, 'to the point' README.md files for advanced developers
- use precise and concise language; avoid generic adjectives like "simple", "lightweight"
- do not include detailed project structure (file/folder listings); high-level module descriptions are acceptable
- never list REST resources in READMEs
- if modules are listed, provide links
- do not use "Orchestrates" term; use more specific alternatives

## Test-Driven Development
- write the test first, then the implementation — TDD is the default workflow
- the test encodes the expected behavior; the implementation makes it pass
- for prototyping and exploration, use the `/prototype` skill instead
- **Red means the test runs and fails on an assertion, not a compile error.** Production code under test must compile — create the minimal stub (class/method signature) the test needs first, then write the test against it. A commit that fails `compileTestJava` is not a valid red commit here: `.githooks/pre-commit` runs `checkstyleTest`, which transitively depends on `compileTestJava` succeeding, so a non-compiling commit is rejected before it can land. Established convention in this repo's history (e.g. 3afc8b38, 38f7c216): the red commit contains a class that compiles but is intentionally incomplete/wrong, so the test fails on assertion.

## Testing Strategy — Three Layers
- **Domain Unit Tests** (`{bc}.domain.*`): test calculations, state transitions, and invariants on the domain POJOs with plain JUnit, no Quarkus runtime, no mocks. Primary home for rich-domain logic tests.
- **Use Case Unit Tests** (`{bc}.application.usecase.*`): plain JUnit + Mockito with mocked out-ports (repositories, gateways), no Quarkus runtime. One test per use-case class covering the orchestration logic.
- **Inbound Adapter Integration Tests** (`{bc}.adapter.in.rest.*`): `@QuarkusTest` + RestAssured with `@InjectMock` on the use case. Test HTTP routing, authorization (`@TestSecurity`), input validation, serialization, and error responses. These verify the API contract.
- **Persistence Adapter Tests** (`{bc}.adapter.out.persistence.*`): `@QuarkusTest` with a real database. Test repository queries, mappers, and transactional behavior. Use `*DbTest` suffix to distinguish from pure unit tests.

## Testing Rules
- unit test methods must not start with "test" or "should"
- avoid writing repetitive or trivial unit tests; keep only essential tests verifying core functionality
- do not write tests for implementations that cannot fail (enums, records, getters/setters)
- create minimalistic tests first
- three tests per class is a starting ceiling for Domain/Use Case unit tests, not a hard cap — don't pad to reach it, and don't split a cohesive Inbound Adapter IT, Persistence `*DbTest`, or fitness-function suite (e.g. one ArchUnit rule per method) just to stay under it
- do not use private visibility in tests

## System Tests (ST)
Not active in this project today — `settings.gradle` defines a single module, no `-st` module exists. The `*IT` classes here (e.g. `DatasetsResourceCrudIT`) are Inbound Adapter Integration Tests per the Testing Strategy above (`@QuarkusTest` + RestAssured, in-module) — a different thing from what this section describes. Apply the rules below only if a genuine multi-module, externally-deployed cross-service test suite gets introduced.

- system tests are created in a dedicated gradle module ending with "-st"
- use microprofile-rest-client for testing JAX-RS resources
- REST client interfaces: src/main/java of the -st module
- test classes: src/test/java of the -st module
- name client interfaces after the resource with "Client" suffix (e.g., GreetingsResource -> GreetingsResourceClient)
- RegisterRestClient configKey: "service_uri"
- STs end with "IT" suffix
- do not use RestAssured. Write e2e test in the -st module

## JAX-RS
- resources should be named in plural (e.g., SpeakersResource not SpeakerResource)
- @Consumes and @Produces should be declared on class-level
- do not implement business logic in JAX-RS resources; delegate to a use case
- prefer returning JAX-RS Response over JsonObject in resources
- do not create new "@RegisterRestClient(configKey," - reuse existing
- JAX-RS-bound types (`FileUpload`, `@RestForm`, `@PartType`) live only in `adapter.in.rest`; they never appear in use-case or port signatures

## JSON Serialization
- use JSON-P for dynamic or polymorphic JSON structures where explicit control over the output is needed
- use JSON-B for typed records and DTOs that map 1:1 to their JSON representation — automatic binding avoids boilerplate
- record entities with dynamic structure should ship with toJSON method returning a JSON-P object
- always map JSON-P in the boundary to entities
- create record entities from JSON-P JsonObject in static method: fromJSON(JsonObject json)

## HTTP Client
- for external REST-shaped services, prefer a MicroProfile/Quarkus REST Client interface (`@RegisterRestClient(configKey = "...")`) over hand-rolled HTTP calls; reuse an existing configKey rather than minting a new one
- fall back to `java.net.http.HttpClient` (synchronous) only for one-off calls that don't fit the REST Client interface shape
- use asynchronous Http APIs (HttpClient.sendAsync) only if explicitly requested

## Project Management
- always ask before changing build.gradle
- on opening existing projects, load AGENTS.md (if present) before making changes
- do not create or change any files on opening existing projects; stop after initialization and wait for instructions
- do not generate code initially in an empty project
- gradle build.gradle must not be created for Java 25 CLI applications
- use quarkus-hibernate-validator for Jakarta Bean Validation when input validation would otherwise require repetitive manual checks
- create metrics and observability features with OTEL / opentelemetry