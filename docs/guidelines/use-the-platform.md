# Use the Platform

## Purpose

The runtime this project already ships on is the default toolbox: the JDK, Jakarta EE /
MicroProfile, Quarkus, and in the browser HTML, CSS and JavaScript as the standards define
them. New code, new dependencies and new mechanisms are justified only where that toolbox
genuinely does not cover the need.

This file is a binding engineering policy: `MUST` is binding, `SHOULD` is a strong default,
and `MUST NOT` is forbidden.

---

## Primary Directive

Before writing code, adding a dependency, or inventing a mechanism, answer in this order:

1. Does the platform already do this?
2. Does something already on this project's classpath or in `package.json` already do this?
3. Is there an established, named pattern for this problem?
4. Only then write something new, and state why the first three did not fit.

---

## Boring Code Wins

- Boring is the target, not the compromise. Code that a new team member reads once and
  understands outranks code that is impressive to read.
- A novel solution MUST be reserved for a problem that has no established pattern. That
  case is rare. Assume the problem is already solved until a look at the platform, this
  codebase and the usual pattern vocabulary comes up empty, and say that you looked.
- Novelty has to be paid for. Whoever writes it also owns the explanation, the
  documentation and the 3am debugging session. Name that cost before writing the code, not
  in the retro.
- "Clever" is a review finding. If a change needs a walkthrough before a reviewer can judge
  it, either simplify it or write down why the complexity carries its weight.

---

## Platform First

- You MUST check the JDK before reaching for a library. `BigDecimal`, `java.time`,
  `HexFormat`, `MessageDigest`, `HttpClient`, `Objects`, `Comparator`, `EnumMap` / `EnumSet`,
  the `Collectors` family and `String.formatted` cover most of what utility libraries are
  pulled in for.
- In the browser you MUST prefer web standards: custom elements, `<template>`, `<dialog>`,
  CSS custom properties, nesting, `:has()`, container queries, `fetch`, `AbortController`,
  `URL` / `URLSearchParams`, `Intl`, `structuredClone`.
- You MUST use the capability the framework already provides rather than a parallel one:
  CDI for wiring, MicroProfile Config for configuration, MicroProfile Fault Tolerance for
  timeouts and retries, Bean Validation for input constraints, JSON-P / JSON-B for JSON.
- A helper that reimplements `String.join`, ISO-8601 parsing, or hex encoding MUST be
  replaced by the platform call. The duplicate carries its own bugs and its own upgrade
  cost for no gain.

## The Bar for a New Dependency

- You MUST NOT add a dependency for work that is a handful of lines of platform code.
- A new dependency MUST clear all of: it solves something the platform genuinely does not,
  it is maintained, its license fits, and the value of the part actually used outweighs the
  transitive cost, the CVE surface and the upgrade burden.
- Adding one MUST be raised explicitly before it lands in `build.gradle` or `package.json`,
  with the alternative that was rejected.

## Established Patterns Over Invention

- You MUST reuse the mechanism this codebase already has before introducing a second one.
  Pagination goes through the existing `PagedResult` conversion and page-count formula; a
  second variant is a regression even when it is locally nicer.
- For problems with a standard answer, you MUST use the standard rather than a house
  version: RFC 9457 problem details for error payloads, IANA link relations for HATEOAS,
  offset or keyset pagination, idempotency keys, transactional outbox, optimistic locking
  via version columns.
- You SHOULD name the pattern in the code and the commit message. A reader who knows the
  pattern then knows the shape without reading the whole file.
- Rule of Three: extract an abstraction on the third occurrence, not the first. Two
  similar call sites are not yet a pattern.

## Flag Anti-Patterns Out Loud

- When the obvious or requested implementation is a known anti-pattern, you MUST say so
  before writing it: name the anti-pattern, name what it costs here, name the pattern that
  replaces it. Then let the decision be made.
- Building it quietly because someone asked for it is not acceptable. Refusing to build it
  is not the answer either. Whoever decides needs the trade-off in front of them, and the
  review record needs it afterwards.
- Frequent offenders in a service like this one: a service class that grew into a god
  object, catching an exception to swallow it, exceptions as control flow, service lookup
  instead of injection, mutable static state, primitive obsession on money and identifiers,
  and business rules leaking into an adapter.

## Testability Is a Design Signal

- A test that is harder to understand than the code it covers is a defect report on the
  code. Fix the code rather than the test.
- You MUST NOT use static mocking, partial mocks, spies, reflection into internals, or
  resetting global state to make a test pass. Each of those says a seam is missing.
- If a single method needs more than a couple of stubbed collaborators, it is doing more
  than one thing. Split it.
- You SHOULD keep decisions pure and push I/O to the edges, so the interesting logic is
  testable with plain values and no test doubles at all. The hexagonal layering in
  `ADR-01` exists to make that the easy path.

---

## Review Checklist

Before finalizing any change, verify:

- Would a new team member follow this on first read, without a walkthrough?
- Does anything here reimplement something the JDK, the browser, or Quarkus already ships?
- Was a dependency added, and does it clear the bar above?
- Does this introduce a second mechanism for something the codebase already solves once?
- Is there a named pattern for this problem that was not used, and why?
- Was any anti-pattern in this change stated openly rather than shipped quietly?
- Is the test easier to read than the code under test?
- Does any test rely on static mocking, spies, or reflection?

If any answer is wrong, revise before shipping.

---

## Final Instruction

When uncertain, prefer the option that:

1. uses what the platform already provides
2. reuses the mechanism this codebase already has
3. follows a pattern a new team member will recognize
4. leaves the smallest amount of code to maintain
5. can be tested without machinery

Familiar and boring outranks clever.
