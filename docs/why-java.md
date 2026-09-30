# Why Java

This document explains the choice of Java for the class management system and
what the rewrite is intended to achieve relative to the JavaScript/Firebase
reference implementation. It is the design rationale the project was built on.

## The context

The reference system is a browser application: HTML/JavaScript on the client,
Firebase Authentication for sign-in, and Cloud Firestore as the database,
with security rules enforcing who may read and write what. That stack works
well when every user has reliable internet and a modern browser. The project's
stated requirements, however, point somewhere else:

1. **No dependencies; pure code that runs and works offline.**
2. **Assume a local server and database may be integrated later**, but do not
   design one now.
3. **Firestore remains an option** because it is secure and convenient for
   sharing data between users — a substitute for operating a local server and
   database — so the project must be *ready* for that integration without
   depending on it.

Java is the language that satisfies all three at once, with the least risk.

## Reason 1 — Offline operation is the default, not an exception

A console application in pure Java has no runtime dependencies beyond a JVM.
There is no `node_modules`, no bundler, no package registry, no CDN, no
browser — nothing that can be missing, outdated, or unreachable when the
network is down. Compiling the whole system needs one command
(`javac -encoding UTF-8 -d out @sources.txt`) and produces class files that
run anywhere a JDK exists.

This directly serves the "internet downtime" scenario that motivated the
project: the school can run the entire system — enrolment, marks, attendance,
timetable, report cards — on a single laptop with no connection at all.

## Reason 2 — The repository seam makes the future integration trivial

The strongest argument for Java here is architectural fit. The system is built
around one interface, `Repository<T>` (see
[architecture.md](architecture.md)):

```java
public interface Repository<T extends Identifiable> {
    T save(T entity);                        // insert or update
    Optional<T> findById(String id);
    List<T> findAll();
    List<T> findBy(Predicate<T> filter);     // maps to a Firestore where-clause
    boolean deleteById(String id);
    long count();
}
```

Every service talks to data exclusively through this contract. The offline
build ships an `InMemoryRepository`; a Firestore adapter only has to implement
the same seven methods, and **no service, UI, or business-rule code changes**.
The in-memory `findBy` filters that services use today correspond one-to-one
with Firestore `where` clauses in an adapter, which is exactly how the
JavaScript version queries its collections.

This is the "designed for integration" requirement made concrete: the decision
about *where* data lives is isolated in one constructor (`Repositories`),
deferred until the local-server/Firestore choice is actually made, and either
path is a small adapter rather than a rewrite.

## Reason 3 — Type safety across a rule-heavy domain

A class management system is dense with invariants that must hold everywhere:
a student belongs to exactly one school; a school has at most one active term;
an assessment is unique per (student, subject, exam type, term); attendance is
unique per (student, date); descriptors are the first band whose [min, max]
contains the score; a finalised timetable rejects new proposals. In
JavaScript these rules live as conventions in functions, enforced by code
review and runtime tests. In Java they are expressed in the type system and
checked at compile time: `EnrollmentStatus` is an enum, not a string that
might be mistyped; a `Term` has `LocalDate` bounds, not strings; the compiler
rejects `repos.assessments.findBy(a -> ...)` against the wrong field. For a
system whose correctness affects real students' records, shifting entire
classes of bugs from runtime to compile time is the single biggest quality
gain of the rewrite.

## Reason 4 — One language for the whole system, end to end

Firebase projects split logic across client JavaScript, security rules,
server-side functions, and ad-hoc admin scripts, each with different tooling.
This rewrite consolidates everything into one process and one language:

- **Business rules** live in the service layer (validation, approvals,
  descriptor resolution, upsert logic) — one authoritative place.
- **Authentication and password storage** are ordinary classes
  (`AuthService`, `PasswordHasher` with salted SHA-256), inspectable and
  testable offline, instead of an external identity provider.
- **Exports** (CSV, ICS, HTML report cards) are generated with `java.nio`
  and string building — no SheetJS, html2pdf, or browser APIs needed.

## Reason 5 — Longevity, performance, and operations

- **Performance.** The workload is small-data, high-frequency (look up a
  student, upsert a mark, render a matrix). A warmed JVM executes this far
  faster than a browser page round-tripping to a cloud database, and startup
  cost is irrelevant for a tool that runs all day on one machine.
- **Longevity.** Java's backwards compatibility means this codebase will
  still compile and run in twenty years, an important property for school
  records. The JDK is one install, present on most institutional machines.
- **Operations.** Backup is copying a directory; deployment is copying class
  files; debugging is reading a stack trace, not reproducing a browser/cloud
  state combination.

## What the rewrite deliberately keeps from the JavaScript version

The rewrite is optimized, not reinvented. Studying the reference project drove
the design:

- **Same roles and flows.** Superadmin, school head, teacher, parent
  (token-based, account-less) — every registration, approval, proposal and
  export flow has a direct counterpart.
- **Same data model.** All 18 entity types and their relationships mirror the
  Firestore collections, with matching id prefixes (`usr-1`, `stu-1`, …) so
  console output and exports stay familiar. See [data-model.md](data-model.md).
- **Same business rules.** Descriptor bands, single active term, assessment
  and attendance upserts, timetable finalisation lock, parent token secrecy,
  notification messages — all preserved.

## Honest trade-offs

- A console UI is less discoverable than a web page; the trade for
  offline-simplicity and zero dependencies was accepted up front.
- In-memory storage is per-process: when the app exits, unsaved changes are
  gone. Persistence arrives with the planned adapter, by design.
- Password hashing uses salted SHA-256 (matching the reference app's approach)
  rather than a memory-hard KDF; upgrading to PBKDF2 is a one-class change in
  `PasswordHasher` when the adapter work begins.

## Summary

Java was chosen because it is the only mainstream platform that delivers all
of: pure-JDK offline operation with zero dependencies; a clean, compile-time
enforced seam where Firestore (or a local server + database) plugs in later
without touching business logic; type safety over a rule-dense domain; and a
single-language codebase that preserves the reference system's roles, data
model and business rules while being faster, more inspectable, and easier to
operate in a low-connectivity school environment.
