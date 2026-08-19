# Architecture Decision Records

Each file records one decision: what was chosen, what the alternatives were, and
what it costs. The point is not the choice but the reasoning — a decision whose
rationale is lost gets re-litigated every six months, or worse, reversed by someone
who only sees the cost.

Read them in whatever order the question demands. If you are starting a project from
this one, ADR-01 through ADR-04 are the load-bearing ones; the rest follow from them.
Read ADR-47 alongside them: the scope it fixes is the first decision a real domain
has to revisit.

**Format:** context, decision, rationale, consequences. Alternatives get their own
section where the rejected option is the one a reader would otherwise reach for.
Numbering is thematic rather than chronological — these describe a shape, not a
history.

**Changing one:** edit it and say what changed. A superseded decision keeps its file
with a `Superseded by ADR-NN` status, because the reasoning that led to it is still
the reason the new one looks the way it does.

## Architecture and layering

| # | Decision |
|---|---|
| [01](ADR-01-hexagonal-architecture.md) | Hexagonal Architecture for the Backend |
| [02](ADR-02-kernel-scope-narrowing.md) | Kernel Scope Narrowing |
| [03](ADR-03-cross-bc-persistence-decoupling.md) | Cross-BC Persistence Decoupling |
| [04](ADR-04-transaction-boundaries-belong-to-use-cases.md) | Transaction Boundaries Belong to Use Cases |
| [47](ADR-47-owner-scoping-as-the-default-read-scope.md) | Owner Scoping Is the Default Read Scope |

## HTTP API

| # | Decision |
|---|---|
| [05](ADR-05-restful-resource-paths.md) | RESTful Resource Paths |
| [06](ADR-06-consistent-query-parameter-naming.md) | Consistent Query Parameter Naming |
| [07](ADR-07-no-api-versioning-via-url-path.md) | No API Versioning via URL Path |
| [08](ADR-08-rfc-9457-problem-details.md) | RFC 9457 Problem Details for Errors |
| [09](ADR-09-bean-validation-for-input.md) | Bean Validation for Input |
| [10](ADR-10-typed-dtos-instead-of-untyped-maps.md) | Typed DTOs Instead of Untyped Maps |
| [11](ADR-11-openapi-annotations-on-all-endpoints.md) | OpenAPI Annotations on All Endpoints |
| [12](ADR-12-configuration-based-base-url-for-absolute-links.md) | Configuration-Based Base URL for Absolute Links |
| [13](ADR-13-empty-page-reports-zero-total-pages.md) | An Empty Page Reports Zero Total Pages |
| [14](ADR-14-rest-exception-mapper-logging-convention.md) | REST Exception Mapper Logging Convention |

## Storage

| # | Decision |
|---|---|
| [15](ADR-15-shared-object-store-abstraction.md) | Shared Object-Store Abstraction |
| [16](ADR-16-s3-type-prefixes-replace-flat-bucket-layout.md) | Type Prefixes Instead of One Bucket per Kind |
| [17](ADR-17-storage-ref-as-a-normalized-table.md) | The Storage Catalogue Is a Normalized Table |
| [18](ADR-18-one-storage-ref-per-upload.md) | One Catalogue Row per Upload, Never Shared |
| [19](ADR-19-hmac-signed-download-tokens.md) | HMAC-Signed Download Tokens in API Responses |
| [44](ADR-44-one-object-store-emulator-for-dev-and-test.md) | One Object-Store Emulator for Dev and Test |

## Resilience and operations

| # | Decision |
|---|---|
| [20](ADR-20-fault-tolerance-annotation-placement.md) | Fault Tolerance Annotation Placement and Timeout Layering |
| [21](ADR-21-readiness-reflects-core-serving-capability.md) | Readiness Reflects Core-Serving Capability Only |
| [22](ADR-22-multi-instance-ready-by-design.md) | Multi-Instance Ready by Design |
| [23](ADR-23-mdc-correlation-and-otel-tracing-config.md) | MDC Correlation and Explicit Tracing Configuration |
| [24](ADR-24-configuration-exclusively-via-environment.md) | Configuration Exclusively via Environment Variables |
| [41](ADR-41-snapshot-cache-with-etag-in-the-cache-entry.md) | Snapshot Cache with the ETag in the Cache Entry |
| [45](ADR-45-local-jwt-validation-instead-of-introspection.md) | Tokens Are Validated Locally, Never Introspected |

## Frontend

| # | Decision |
|---|---|
| [25](ADR-25-frontend-served-by-dedicated-nginx-container.md) | Frontend Served by a Dedicated Nginx Container |
| [26](ADR-26-auth-initialization-centralized.md) | Auth Initialization Centralized in the Entry Module |
| [27](ADR-27-frontend-token-refresh-strategy.md) | Frontend Token-Refresh Strategy |
| [28](ADR-28-dispatcher-functions-in-the-control-layer.md) | All Dispatcher Functions Belong in the Control Layer |
| [29](ADR-29-global-toast-container-as-a-singleton.md) | Global Toast Container as a Singleton Component |
| [30](ADR-30-notifications-redux-slice-owns-toast-state.md) | The Notifications Slice Owns Toast State |
| [31](ADR-31-error-toasts-automatic-success-explicit.md) | Error Toasts Are Automatic, Success Toasts Are Explicit |
| [46](ADR-46-frontend-i18n-via-bundled-flat-catalogues.md) | Frontend i18n via Bundled Flat Catalogues |

## Pipeline and deployment

| # | Decision |
|---|---|
| [32](ADR-32-github-actions-on-free-tier.md) | GitHub Actions on the Free Tier as CI Platform |
| [33](ADR-33-path-filtered-per-job-execution.md) | Path-Filtered Jobs, PR and main Triggers Only |
| [34](ADR-34-testcontainers-for-integration-tests-in-ci.md) | Real Dependencies for Integration Tests |
| [35](ADR-35-external-service-ports-for-test-isolation.md) | Fixed, Distinct Ports for Test Dependencies |
| [36](ADR-36-compose-devservices-disabled-by-default-in-test.md) | Compose-Based Dev Services Off by Default in Tests |
| [37](ADR-37-self-hosted-registry-for-all-images.md) | One Registry for All Container Images |
| [38](ADR-38-promote-tested-artifacts-into-images.md) | Promote Tested Artifacts into Images, Never Rebuild |
| [39](ADR-39-deploy-as-a-self-contained-deployment-unit.md) | deploy/ Is a Self-Contained Deployment Unit |
| [40](ADR-40-remote-deployment-via-rsync-and-ssh.md) | Remote Deployment via rsync and SSH |
| [42](ADR-42-security-headers-at-the-proxy-csp-at-the-workload.md) | Security Headers at the Proxy, CSP at the Workload |
| [43](ADR-43-capacity-tests-in-repo-as-a-gatling-module.md) | Capacity Tests Live In-Repo as a Gatling Module |
