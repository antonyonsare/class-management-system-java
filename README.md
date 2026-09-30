# Circular Digital Bridge — Class Management System (Java)

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](./LICENSE)
[![Repo size](https://img.shields.io/github/repo-size/antonyonsare/class-management-system-java)](https://github.com/antonyonsare/class-management-system-java)

A complete, offline-capable rewrite of the Circular Digital Bridge class management
system in pure Java. The original system is a browser app backed by Firebase;
this version reproduces every role, workflow and business rule as a console
application with zero external dependencies — only the JDK is required.

Table of contents
- [Why this exists](#why-this-exists)
- [Requirements](#requirements)
- [Quick start](#quick-start)
- [Demo mode](#demo-mode)
- [What it does (roles & features)](#what-it-does-roles--features)
- [Project layout](#project-layout)
- [Documentation](#documentation)
- [Exports & Output formats](#exports--output-formats)
- [Contributing](#contributing)
- [License & Contact](#license--contact)
- [Troubleshooting / FAQ](#troubleshooting--faq)

## Why this exists
The Java version is an efficient, optimized counterpart to the JavaScript reference
implementation, designed for schools that need the system to run without internet
access and without a browser. The full reasoning — including why Java was chosen
— is in [docs/why-java.md](docs/why-java.md).

## Requirements
- JDK 17 or later (tested on JDK 25)
- No external libraries or build tools required
- POSIX shell for the provided Unix scripts; Windows batch for Windows users


Manual compile & run (any platform with JDK):
```bash
# compile (Unix example)
javac -encoding UTF-8 -d out $(find src -name "*.java")

# run the app (class path = out)
java -cp out com.cdb.cms.Main --demo
```

## Demo mode
`--demo` preloads one fully populated school (Greenhill Academy) with accounts,
classes, students, marks, attendance, a partly-built timetable, tasks and events.
When you run with `--demo` the app prints the demo credentials and sample parent token.

Default demo credentials shown on startup:
| Role | Login | Password |
|---|---:|---|
| Superadmin | `admin@cdb.test` | `admin123` |
| School head | `head@greenhill.test` | `head123` |
| Teacher | `peter@greenhill.test` | `teach123` |
| Teacher | `mary@greenhill.test` | `teach123` |
| Parent | printed **sample parent token** | — |

Notes:
- School join codes (e.g. `SCH4266`) and parent tokens are randomly generated on each run.
- Without `--demo`, the first launch asks you to create the superadmin account and starts empty.

## What it does (roles & features)
Four roles, mirroring the web reference:

- Superadmin — approve school-head/teacher registrations, browse schools, send reminders,
  view platform statistics, export a schools CSV.
- School head — approve teachers/enrolments, manage classes/subjects/terms/exam types,
  approve teacher timetable proposals, finalize (lock) timetables, assign tasks, write comments,
  maintain calendar, view analytics, export students CSV.
- Teacher — enter marks (with automatic grade descriptors), take attendance, propose timetable slots,
  propose new classes/subjects, manage tasks, read notifications.
- Parent — no account: use an unguessable token to view read-only report card, weekly timetable,
  calendar; export printable HTML report card and ICS calendar feed.

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
- [docs/why-java.md](docs/why-java.md) — reasoning and benefits
- [docs/architecture.md](docs/architecture.md) — architecture, layers, data flow
- [docs/data-model.md](docs/data-model.md) — entities and mapping to Firestore
- [docs/user-guide.md](docs/user-guide.md) — walkthrough for each role
- [docs/developer-guide.md](docs/developer-guide.md) — building, testing, adding an adapter

## Exports & output formats
- CSV (RFC 4180) — opens in Excel/LibreOffice
- ICS — calendar import for Outlook/Google
- HTML — printable report cards (print to PDF from any browser)
All exports are library-free (pure JDK).

## Contributing
Contributions are welcome.

Suggested workflow:
1. Fork the repo and create a feature branch
2. Make changes and run the compile scripts to ensure no regressions
3. Open a pull request with a clear description and test steps

Add a CONTRIBUTING.md with specifics such as:
- Code style and formatting conventions
- Commit message style (conventional commits)
- How to add a Firestore/local DB adapter (refer to docs/developer-guide.md)
- How to run any tests (future)
