# How the images are produced

## Normally: not by hand

Both images are produced by the CI pipeline on merge to the default branch. There is
no manual step in the normal flow.

The pipeline's shape is the point (ADR-38): the job that runs the tests also produces
the deployable artifact and uploads it, and the image job downloads that artifact and
copies it into an image. The image Dockerfiles contain no compilation step at all.

That gives one guarantee worth having: **the bytes that passed the tests are the bytes
in the image.** A pipeline that compiles a second time inside the container build
ships an artifact nothing tested — usually identical, and the times it is not are
exactly the times it matters.

```
test job ──► artifact ──► image job ──► registry ──► deploy.sh pulls a tag
   │                          │
   └── tests run here         └── FROM / COPY / ENTRYPOINT, nothing else
```

Images land at `${REGISTRY}/ingoschindler/starter-backend:jvm-<sha>` and
`…/starter-frontend:<sha>`, each also tagged `latest` (ADR-37).

## By hand, when you need to

Same order as the pipeline: assemble first, then package. The Dockerfile says so in
its header, because the failure otherwise is a confusing missing-directory error.

**Backend:**

```bash
./gradlew quarkusBuild
docker image build -f src/main/docker/Dockerfile.jvm.prod \
  -t registry.example.com/ingoschindler/starter-backend:local .
```

**Frontend:**

```bash
cd web-app && npm ci && npm run build
docker image build -f Dockerfile -t registry.example.com/ingoschindler/starter-frontend:local .
```

## Native image

The configuration is present (`%native` profile in `application.properties`) and
unused by default.

```bash
./gradlew quarkusBuild -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

Roughly: JVM ~200 MB, boots in a second or two, more memory at runtime; native ~50 MB,
boots in tens of milliseconds, less memory, and a compile measured in minutes plus a
reflection-registration problem for every library that uses it dynamically.

JVM is the default here because startup time is not a constraint for a long-running
service, and the native compile cost is paid on every pipeline run. Native earns its
cost where instances start and stop constantly — scale-to-zero, or a per-request
runtime.

## Layer ordering

Four `COPY` steps, coarsest first: dependencies, then the application's own classes.
One `COPY` of the whole output directory would invalidate the dependency layer on
every commit, which is most of the image and most of the push.

## Registry cleanup

A registry with no retention policy grows until the disk fills, on the machine that
also serves production.

```bash
REGISTRY_USER=… REGISTRY_PASSWORD=… ./cleanup-registry.sh
```

Keeps the N most recent versioned tags per repository plus anything tagged `latest`.
It resolves each tag to its manifest digest before deleting — deleting by tag leaves
the layers behind, which is how a "cleaned" registry stays full. Needs delete enabled
on the registry server, which is off by default.
