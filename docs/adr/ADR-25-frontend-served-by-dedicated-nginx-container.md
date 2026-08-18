# ADR-25: Frontend Served by a Dedicated Nginx Container

**Status:** Accepted
**Reversibility:** medium — routing and one image.

## Context

A single-page application has to be served from somewhere. The frictionless option
is to put the built assets into the backend's static-resource directory and add a
catch-all route that returns `index.html` for anything the API does not match.

It works, and it couples two things that have nothing in common. The frontend and
the backend then share a release cadence, so a CSS fix waits for a backend
deployment. They share an image, so the backend image grows and its layer cache is
invalidated by frontend changes. And the catch-all route is a permanent hazard: it
sits in front of the API's own 404 handling, so a mistyped API path returns an HTML
page with status 200 instead of a problem-details 404.

Static asset serving from an application server is also strictly worse — no
`immutable` caching for hashed filenames, no compression tuned for text assets, and
a request thread spent on a file.

## Decision

The frontend is its own Nginx container, built from its own image. The reverse
proxy routes by path at the same host: `/api`, `/q` and `/.well-known` to the
backend, everything else to Nginx.

The backend has no static-resource directory and no catch-all route.

Nginx owns what Nginx is good at:

- `immutable` far-future caching for hashed asset filenames, and `no-cache` for
  `index.html` — a deploy ships new assets, and a cached entry document would mean
  nobody asks for them
- `try_files ... /index.html` so client-side routes resolve, scoped to the paths
  the proxy actually sends here
- gzip for text assets
- the Content-Security-Policy, because a `script-src` policy belongs to whatever
  serves the scripts (ADR-42)

Development mirrors the same routing through a small proxy container, so the
browser sees one origin locally too. Two origins in development means CORS and
cookie behaviour that exists nowhere else, which is the class of problem that first
appears after a deploy.

## Rationale

Same host, so no CORS and no cross-origin cookie questions. Separate containers, so
separate release cadences and separate caches. The only cost is one more image in
the pipeline.

## Consequences

- Two images to build and push. Both are produced from tested artifacts by the same
  reusable workflow (ADR-38).
- The router priority matters: the backend's rule is higher-priority than the
  frontend's catch-all. Getting it backwards serves HTML for API calls, so it is
  written down in the compose labels.
- The CSP's allowed auth origin differs per deployment, so the vhost is a template
  rendered at container start from the environment rather than a static file.

## Related

- ADR-42 — the split between proxy headers and the workload's CSP
- ADR-05 — the path prefixes the routing depends on
