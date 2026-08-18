# ADR-37: One Registry for All Container Images

**Status:** Accepted
**Reversibility:** medium — image references in the deployment.

## Context

Images have to live somewhere the deployment host can pull them. The options are a
public registry, the platform's own package registry, or a self-hosted one.

Split registries are the failure to avoid: some images in one place, some in
another, because each was added at a different time. Then the deployment needs
credentials for both, a pull failure has two possible causes, and a cleanup policy
has to be written twice.

## Decision

One registry for every image this project produces, configured in a single place and
referenced through variables:

```yaml
image: ${REGISTRY}/${BACKEND_IMAGE_NAME}:${BACKEND_IMAGE_TAG}
```

Registry, name and tag are all variables, so the same compose file runs against a
different registry without editing.

Credentials are one pair of pipeline secrets, used by the reusable push workflow
(ADR-38).

**Retention is explicit.** A registry with no cleanup grows until the disk fills,
and it fills on the machine that also serves production. A cleanup script keeps the
N most recent versioned tags per repository plus anything tagged `latest`, and it
resolves each tag to its manifest digest before deleting — deleting by tag leaves
the layers behind, which is how a "cleaned" registry stays full.

## Rationale

One registry means one credential, one failure mode and one retention policy.
Self-hosted, in this case, because the deployment host and the registry are already
adjacent and the images are not public.

## Consequences

- The registry is infrastructure that has to exist before a deployment works. It is
  named in the deployment documentation as a prerequisite.
- The cleanup script needs delete enabled on the registry server, which is off by
  default. That is noted in the script's own header, since the failure otherwise
  looks like a permission problem.

## Related

- ADR-38 — what gets pushed
- ADR-39 — what pulls it
