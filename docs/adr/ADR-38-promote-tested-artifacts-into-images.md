# ADR-38: Promote Tested Artifacts into Images, Never Rebuild

**Status:** Accepted
**Reversibility:** high — pipeline structure.

## Context

The obvious image pipeline compiles inside the container build: `COPY . .` then run
the build tool. It is self-contained and needs no artifact handling.

It also means what ships is not what was tested. The test job compiled one artifact;
the image job compiled a second one, at a different time, from a freshly resolved
dependency tree. Usually identical. When it is not — a dependency that moved, a
different toolchain minor version — the difference is between a green pipeline and
what actually runs.

It is also slow: the same compilation runs twice, and the second one has no
dependency cache.

## Decision

The pipeline has one place where the application is assembled, and it is the job
that runs the tests.

1. The test job runs the tests **and** produces the deployable artifact, then
   uploads it.
2. The image job downloads that artifact and copies it into an image. Its Dockerfile
   contains no compilation step — only `FROM`, `COPY` and `ENTRYPOINT`.
3. The image is pushed with an immutable tag plus a moving `latest`.
4. The deployment pulls a tag. It never builds (ADR-39).

The Dockerfile states the expectation in its header, so anyone building by hand
knows to produce the artifact first.

Layers are ordered coarsest-first — dependencies, then application classes — so an
application change does not invalidate the dependency layer.

## Rationale

The guarantee is the point: the bytes that passed the tests are the bytes in the
image. That is not achievable with a build-inside-the-image pipeline, no matter how
reproducible it looks.

The speedup is a side effect, and a large one, since the container build no longer
needs a toolchain or a dependency cache.

## Consequences

- The image job depends on the test job's artifact, so the pipeline has a real
  dependency edge. A failed test job means no image, which is correct.
- Artifact retention is short — one day — since the only consumer is the next job.
- Building an image locally requires the artifact step first. Stated in the
  Dockerfile.

## Related

- ADR-32 — the pipeline this shape is designed for
- ADR-37 — where the image goes
- ADR-39 — what consumes it
