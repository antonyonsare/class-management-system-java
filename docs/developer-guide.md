# Developer guide

Build, run, test, and extend the system. The only prerequisite is a JDK
(developed on JDK 21+; no Maven, Gradle, or any third-party library).

## Build and run

```bash
# Windows
compile.bat          # javac -encoding UTF-8 -d out @sources.txt
run.bat --demo       # compiles first if needed, then runs

# Linux / macOS
./compile.sh
./run.sh --demo
```

Equivalent by hand, from the project root:

```bash
find src -name '*.java' > sources.txt        # or: (for /r src %f in (*.java) do echo %f) > sources.txt
javac -encoding UTF-8 -d out @sources.txt
java -cp out com.cdb.cms.Main --demo
```

`-encoding UTF-8` matters: source files contain em-dashes and box-drawing
characters. `Main` also forces UTF-8 on `System.out` so those characters
render correctly even when the host console defaults to a legacy charset.

## Project layout

```
src/com/cdb/cms/
├── Main.java               entry point; --demo flag, UTF-8 stdout fix
├── model/                  18 entities + 6 enums, one class per collection
├── repository/
│   ├── Identifiable.java   getId/setId contract
│   ├── Repository.java     the persistence seam (interface)
│   ├── InMemoryRepository.java   offline implementation
│   └── Repositories.java   DI container: one repository per entity
├── service/                business rules, one service per role + analytics/export
│   └── ServiceException.java     user-facing rule violations
├── dto/                    read models for analytics screens
├── util/                   PasswordHasher, Tokens, CsvWriter, ICS/ReportCard writers
└── ui/                     Console I/O gateway + App composition root + 4 role consoles
```

The dependency direction is strictly one-way:
`ui → service → repository → model`. Services never touch the console;
the UI never contains business rules.

## Testing without a test framework

There is no JUnit dependency (pure-JDK constraint), so verification is done
by scripting the console: pipe stdin into the app and assert on stdout.

```bash
# Full scripted teacher session: login, enter marks, exit
printf '1\npeter@greenhill.test\nteach123\n2\n1\n1\n1\n55\n65\n78\n\n0\n' | \
  java -cp out com.cdb.cms.Main --demo
```

Each `\n`-separated token is one answer to one prompt (`1` = log in,
then email, password, *Enter marks*, class 1, subject 1, exam 1, then three
scores and a blank to skip, then `0` = back). This is how every
role, every business rule (descriptor resolution, upserts, the finalisation
lock), and every export was verified end-to-end during development. Two
harness details to copy if you build your own scripts:

- Drive the app through a real pipe (`cat fifo | java ...`), not a file
  redirect, when running under MSYS/Git-Bash — the native JVM sees a
  redirected file as an immediate EOF.
- Interactive sessions that must *react* to output (e.g. the demo prints a
  fresh parent token each run) need a feedback loop: write input to a named
  pipe, poll the output file for the token with `sed`, then feed it in.

Graceful shutdown on end-of-input is handled in-app:
`Console.readLine` throws `Console.EndOfInput` when stdin closes, and
`App.run` catches it and exits with `Goodbye.`

## The integration seam: adding Firestore or a local database

Everything persistence-related funnels through one interface
(`src/com/cdb/cms/repository/Repository.java`):

```java
public interface Repository<T extends Identifiable> {
    T save(T entity);                       // upsert; assigns id when absent
    Optional<T> findById(String id);
    List<T> findAll();
    List<T> findBy(Predicate<T> filter);    // maps to a where-clause
    boolean deleteById(String id);
    long count();
}
```

and one construction site — the `Repositories` constructor, which currently
contains eighteen lines of `new InMemoryRepository<>("usr")` …
`new InMemoryRepository<>("aud")`.

### Recipe for a Firestore adapter

1. **Implement the interface per collection.** A
   `FirestoreRepository<T extends Identifiable>` backed by
   `FirebaseFirestore` with a `DocumentReference` per entity; each entity
   also needs a to/from-Map (or POJO) mapper. Example for one collection:
   `FirestoreRepository<Student> students = new FirestoreRepository<>(
   firestore.collection("students"), Student.class);`

2. **Translate `findBy` into queries.** Most service filters are simple
   field comparisons — `e.getSchoolId().equals(schoolCode)`,
   `ProposalStatus.PENDING == e.getStatus()` — which map one-to-one onto
   Firestore `whereEqualTo` clauses. Either keep the `Predicate` and filter
   client-side (simplest, fine at school scale), or overload the adapter
   with typed query methods as the need arises. The services that matter
   most: `students by classId`, `assessments by (studentId, termId)`,
   `timetableSlots by schoolId+status`, `users by email`.

3. **Delegate authentication to Firebase Auth.** The offline build stores
   salted SHA-256 hashes in `User.passwordHash` because it has no Firebase.
   Under Firestore, login becomes a Firebase Auth call and `passwordHash`
   is simply never read or written by the adapter; `User` documents then
   hold only profile data (name, role, schoolId, superHead). This matches
   the reference web app exactly.

4. **Swap the construction lines.** Replace the eighteen
   `InMemoryRepository` allocations in `Repositories()` with your adapter
   instances. Nothing else in the codebase changes — services receive the
   same `Repository<T>` references through the same constructor injection.

A future **local server + database** variant follows the identical recipe:
implement `Repository<T>` over your storage (JDBC, SQLite, files), drop it
into `Repositories`, done. The constraint that made this possible is that
the interface has no persistence-specific leakage — no transactions, no
query builders, no IDs beyond strings.

### What an adapter must preserve

These invariants are the system's contract with its data; an adapter that
breaks them breaks the business rules that rely on them:

- **Upsert uniqueness** — one assessment per (student, subject, examType,
  term); one attendance row per (student, date). Enforce with composite
  document ids or unique constraints.
- **Single active term** per school.
- **Timetable finalisation lock** — once `School.timetableFinalized` is
  true, no new `TimetableSlot` documents.
- **School code as school id** — `schoolId` foreign keys everywhere carry
  the school code.
- **Id readability** — `InMemoryRepository` ids (`usr-1`) exist only for
  console readability; a server adapter may use its own id scheme (the web
  app uses Firestore auto-ids) without affecting logic.

## Extending the application

- **New business rule** → the service that owns the domain area. Throw
  `ServiceException` with a user-readable message; the console catches it
  and prints `Failed: <message>`.
- **New screen** → add a private method to the role's console and a menu
  entry; keep all I/O through `Console` so the UTF-8/EOF handling stays
  central.
- **New entity/collection** → model class + enum fields if any, one line in
  `Repositories`, service methods, and (for docs) one row in
  [data-model.md](data-model.md).
- **New export** → follow `ExportService` + the writers in `util/`
  (`CsvWriter`, the ICS and report-card writers) — plain `java.io`, no
  libraries.

## Known deliberate limitations

- Data lives per-process: restart the app and the in-memory store is empty
  (this is the offline design; persistence arrives with the adapter).
- Password hashing is salted SHA-256 — adequate for an offline demo, not a
  substitute for Firebase Auth's KDF-backed storage; see the trade-off
  discussion in [why-java.md](why-java.md) and the decisions list in
  [architecture.md](architecture.md#key-decisions).
- Parent tokens are shown once at enrollment; there is no recovery flow in
  the console build (the web app regenerates them server-side).
