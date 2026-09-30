# Circular Digital Bridge — Class Management System (Java)

A complete, offline-capable rewrite of the Circular Digital Bridge class
management system in pure Java. The original system is a browser app backed by
Firebase (Firestore + Authentication); this version reproduces every role,
workflow and business rule as a console application with **zero external
dependencies** — only the JDK is required.

| | |
|---|---|
| **Language** | Java (any JDK 17+, developed on JDK 25) |
| **Dependencies** | None. Pure JDK: no Maven, no Gradle, no jars |
| **Persistence** | In-memory repositories (offline); swappable for Firestore or a local DB through one interface |
| **Interface** | Console UI with per-role menus |

## Why this exists

The Java version is an efficient, optimized counterpart to the JavaScript
reference implementation, designed for schools that need the system to run
without internet access and without a browser. The full reasoning — including
why Java was chosen — is in [docs/why-java.md](docs/why-java.md).

## Quick start

```bash
# Windows
compile.bat          # compiles all sources into out/
run.bat --demo       # compiles (first time) then runs with demo data

# Linux / macOS
./compile.sh
./run.sh --demo
```

Or by hand, with any JDK:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")   # Unix
java -cp out com.cdb.cms.Main --demo
```

### Demo mode

`--demo` preloads one fully populated school (Greenhill Academy) with
accounts, classes, students, marks, attendance, a partly-built timetable,
tasks and events, then prints the credentials:

| Role | Login | Password |
|---|---|---|
| Superadmin | `admin@cdb.test` | `admin123` |
| School head | `head@greenhill.test` | `head123` |
| Teacher | `peter@greenhill.test` | `teach123` |
| Teacher | `mary@greenhill.test` | `teach123` |
| Parent | the printed **sample parent token** | — |

The school join code (e.g. `SCH4266`) and parent tokens are randomly generated
on every run. Without `--demo`, the first launch asks you to create the
superadmin account and starts empty, exactly like the web app's first run.

## What it does

Four roles, mirroring the web reference:

- **Superadmin** — approves school-head and teacher registrations, browses the
  schools directory, sends reminders to heads, views platform statistics,
  exports a schools CSV.
- **School head** — approves teachers and enrolment requests, manages classes,
  subjects, terms, exam types and grade descriptors, approves or rejects
  teacher-proposed timetable slots and class/subject proposals, finalises the
  timetable (locking it), assigns tasks, writes overall comments, maintains the
  school calendar, views analytics, exports a students CSV.
- **Teacher** — enters marks (with automatic grade descriptors), takes
  attendance, proposes timetable slots, proposes new classes/subjects, manages
  tasks, reads notifications.
- **Parent** — no account: enters the student's unguessable token and sees a
  read-only report card, weekly timetable, school calendar, and can export a
  printable HTML report card and an ICS calendar feed.

Exports are library-free: **CSV** (opens in Excel), **ICS** (imports into
Outlook/Google Calendar) and **HTML** report cards (print to PDF from any
browser) — the offline replacements for SheetJS, html2pdf and the browser ICS
builder used by the web app.

## Project layout

```
src/com/cdb/cms/
  Main.java               entry point (--demo flag)
  model/                  18 entity classes + 6 enums (plain JavaBeans)
  repository/             Repository<T> contract, InMemoryRepository, Repositories (DI container)
  service/                AuthService, SuperAdminService, SchoolHeadService,
                          TeacherService, AnalyticsService, ParentService,
                          ExportService, Notifier
  dto/                    read-only data shapes for reports and analytics
  ui/                     App (composition root + top-level loop), per-role
                          consoles, Console helpers, TimetableGrid, DemoDataLoader
  util/                   PasswordHasher (salted SHA-256), CsvWriter (RFC 4180)
```

## Documentation

| Document | Contents |
|---|---|
| [docs/why-java.md](docs/why-java.md) | The reasoning behind choosing Java, and what the rewrite gains |
| [docs/architecture.md](docs/architecture.md) | Layers, the repository seam, data flow, key decisions |
| [docs/data-model.md](docs/data-model.md) | Every entity and how it maps to the Firestore collections |
| [docs/user-guide.md](docs/user-guide.md) | Walkthrough for each role |
| [docs/developer-guide.md](docs/developer-guide.md) | Building, testing, and adding a Firestore (or local DB) adapter |
