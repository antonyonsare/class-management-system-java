# Architecture

## Overview

The system follows a strict four-layer design, with dependencies pointing
downward only:

```
┌─────────────────────────────────────────────────────────────┐
│ ui        App, SuperAdminConsole, SchoolHeadConsole,        │
│           TeacherConsole, ParentConsole, Console,           │
│           TimetableGrid, DemoDataLoader                     │
├─────────────────────────────────────────────────────────────┤
│ service   AuthService  SuperAdminService  SchoolHeadService │
│           TeacherService  AnalyticsService  ParentService   │
│           ExportService  Notifier                           │
├─────────────────────────────────────────────────────────────┤
│ dto       StudentMetrics  AnalyticsMatrix  MatrixRow        │
│           ClassPerformance  DashboardStats                  │
├─────────────────────────────────────────────────────────────┤
│ repository Repository<T>  InMemoryRepository  Repositories  │
├─────────────────────────────────────────────────────────────┤
│ model     18 entities + 6 enums (no dependencies upward)    │
└─────────────────────────────────────────────────────────────┘
```

- **model** — Plain JavaBeans (`User`, `Student`, `Assessment`, …) plus enums
  (`Role`, `AccountStatus`, `EnrollmentStatus`, `ProposalStatus`,
  `AttendanceStatus`, `DayOfWeek`). No logic beyond trivial accessors, so the
  same shapes can be serialized to any future store.
- **repository** — The persistence seam. `Repository<T extends Identifiable>`
  is the only interface services know about.
- **service** — All business rules. Services receive `Repositories` (and each
  other) via constructors and never construct dependencies themselves.
- **dto** — Read-only shapes assembled by `AnalyticsService`/`ExportService`
  for reports; they decouple what the UI renders from how data is stored.
- **ui** — The composition root (`App`) wires repositories to services, then
  drives the top-level loop: first-run superadmin bootstrap, login,
  registrations, parent-token access, and routing to the per-role console —
  the counterpart of the web app's `auth.js` redirecting to
  superadmin/schoolhead/teacher/parent pages.

## The repository seam (the integration point)

```java
public interface Repository<T extends Identifiable> {
    T save(T entity);
    Optional<T> findById(String id);
    List<T> findAll();
    List<T> findBy(Predicate<T> filter);
    boolean deleteById(String id);
    long count();
    // defaults: findFirst(filter), exists(filter), count(filter)
}
```

`Repositories` is a dependency-injection container holding one repository per
entity type, mirroring the reference app's Firestore collections with the same
document-id prefixes (`usr`, `sch`, `stu`, …). Services depend on the
container, so swapping persistence is a one-line change per collection in a
single constructor. The shipped `InMemoryRepository` keeps insertion order
(`LinkedHashMap`) and generates ids as `<prefix>-<n>`.

`findBy(Predicate<T>)` is the deliberate bridge to a query store: every filter
written against the in-memory implementation describes a `where` clause in a
Firestore adapter, so query logic porting is mechanical.

## Composition root

`App` constructs the object graph exactly once:

```java
repos       = new Repositories();
notifier    = new Notifier(repos);
auth        = new AuthService(repos);
superAdmin  = new SuperAdminService(repos, notifier);
headService = new SchoolHeadService(repos, notifier);
teacherService = new TeacherService(repos);
analytics   = new AnalyticsService(repos);
parentService = new ParentService(repos, auth, analytics);
exports     = new ExportService(repos);
```

`Main` decides only one thing — whether `DemoDataLoader` preloads the demo
school — then calls `App.run()`.

## Data flow examples

**Entering a mark (teacher):**
`TeacherConsole` → `TeacherService.enterMark(...)` → validates teacher/
student/subject/exam/term linkage → upserts the `Assessment` in
`repos.assessments` (one row per student+subject+examType+term) → resolves the
grade descriptor from the school's `GradeDescriptor` bands → returns the
saved assessment; the console prints `saved: 78 -> Meeting Expectation`.

**Parent report card:**
`ParentConsole` → `ParentService.metrics(student)` → `AnalyticsService`
computes attendance rate, term average, remarks (head comment, else the
descriptor band covering the average), class teacher/head names and per-subject
scores into a `StudentMetrics` DTO → the console renders it; `ExportService`
can turn the same DTO into printable HTML.

**Timetable finalisation (the lock):**
`SchoolHeadService.finaliseTimetable` flips `School.timetableFinalized` and
audits the action; afterwards `TeacherService.proposeSlot` rejects new
proposals — the rule lives in the service, not the UI, so every future
interface (GUI, server API) inherits it.

## Key decisions

- **Services own every business rule** (approval state machines, upsert
  keys, descriptor resolution, the finalisation lock, single-active-term
  invariant). The UI only formats and prompts. This mirrors where the rules
  sat in the Firebase security rules + service functions of the reference,
  but in one inspectable place.
- **`ServiceException`** signals business-rule failures; consoles catch it
  and print the message. No checked exceptions leak into the UI except for
  real I/O in exports (`IOException` from `Files.createDirectories`), which
  consoles handle alongside `RuntimeException`.
- **Passwords** are salted SHA-256 (`util/PasswordHasher`); plaintext never
  appears in any entity or log.
- **Parent access is token-based, not account-based**, exactly like the web
  app: a student carries an unguessable `parentToken` (two random base36
  segments); knowing it is the only credential.
- **`Console` is the single I/O gateway** for the UI layer: validated
  prompts, numbered menus, fixed-width tables, password reading without echo
  (with a plain-line fallback when no console is attached, e.g. under
  piping). `Main` forces UTF-8 on stdout so symbols and em-dashes render on
  legacy Windows code pages, and end-of-input on stdin shuts the app down
  gracefully instead of looping.
- **No threads, no globals.** The console UI is single-threaded; entities are
  plain objects; state lives in repositories. Concurrency will come from the
  future store (Firestore), not from this codebase.
- **Exports are library-free.** `CsvWriter` implements RFC 4180 quoting;
  `ExportService` builds ICS calendars and self-contained HTML report cards
  as strings — no SheetJS/html2pdf equivalents required.

## Testing approach

With no test framework (zero-dependency constraint), verification is
script-driven end-to-end: the demo dataset is fully deterministic except for
generated school codes and parent tokens, so each role's console is driven
with piped stdin and its output and export files inspected. All four roles,
the analytics matrix, timetable finalisation and the teacher lockout, and the
CSV/ICS/HTML exports have been exercised this way; see
[developer-guide.md](developer-guide.md).
