# Data model

Eighteen entities, mirroring the Firestore collections of the reference web
app one-for-one. The offline build stores them in
[`InMemoryRepository`](../src/com/cdb/cms/repository/InMemoryRepository.java)
instances; the document id in the offline build is a readable string
(`<prefix>-<n>`, assigned on save) that plays the role the Firestore document
id plays in the web app.

For each entity below: the Firestore collection it maps to, the offline id
prefix used in `Repositories`, and the fields with their meaning. The last
section maps the two storage models onto each other.

## Identifiers

Every entity implements
[`Identifiable`](../src/com/cdb/cms/repository/Identifiable.java)
(`getId`/`setId`). Two identifier conventions coexist, deliberately:

- **Server-generated ids** — most entities get their id from the repository
  on first save (`stu-1`, `asm-1`, ...).
- **Natural-key ids** — entities that had a natural unique key in the web app
  keep it as the document id: `School.schoolCode` (assigned at creation from
  the school's name, `SCH` + 4 digits) and, by extension, the
  `schoolId` foreign key everywhere. This matches the reference app, where the
  school code *is* the school document id.

Foreign keys are plain string fields holding another document's id — exactly
as in Firestore.

## users

Id prefix: `usr` · Firestore: `users`

| Field        | Type            | Notes                                                        |
|--------------|-----------------|--------------------------------------------------------------|
| `id`         | `String`        |                                                              |
| `name`       | `String`        |                                                              |
| `email`      | `String`        | Login identifier; unique.                                    |
| `passwordHash` | `String`      | Salted SHA-256 (`salt:hex`). **Offline-only**: a Firestore adapter ignores this field and delegates authentication to Firebase Auth. |
| `role`       | `Role`          | `SUPERADMIN`, `HEAD`, `TEACHER`.                             |
| `status`     | `AccountStatus` | `PENDING`, `APPROVED`, `REJECTED`. Only `APPROVED` accounts can log in — a `PENDING` account still awaits superadmin approval. |
| `schoolId`   | `String`        | Null for the superadmin (no school).                         |
| `superHead`  | `boolean`       | True for the founding head of a school — the one who created it. |
| `createdAt`  | `LocalDateTime` |                                                              |

## schools

Id prefix: `sch` · Firestore: `schools` — **document id is `schoolCode`**, not
a generated id.

| Field                 | Type            | Notes                                                   |
|-----------------------|-----------------|---------------------------------------------------------|
| `schoolCode` (= `id`) | `String`        | `SCH` + 4 digits, derived from the school name at creation. |
| `name`                | `String`        |                                                         |
| `superHeadId`         | `String`        | User id of the founding head.                           |
| `motto`               | `String`        |                                                         |
| `email`               | `String`        |                                                         |
| `phone`               | `String`        |                                                         |
| `address`             | `String`        |                                                         |
| `timetablePeriods`    | `List<String>`  | Period labels, e.g. `[8:00–8:40, 8:40–9:20, ...]`. Timetable cells are indexed 1..n into this list. |
| `timetableFinalized`  | `boolean`       | Once true, no timetable slot can be proposed.           |
| `createdAt`           | `LocalDateTime` |                                                         |

## schoolHeadApprovals

Id prefix: `sha` · Firestore: `schoolHeadApprovals` (field `headApprovals` in
`Repositories`)

A request from a user asking to register a new school and become its head.
Approved → school + head account created; rejected → archived for audit.

| Field         | Type            | Notes                          |
|---------------|-----------------|--------------------------------|
| `id`          | `String`        |                                |
| `uid`         | `String`        | Requesting user id (or "pending" while no account exists). |
| `name`        | `String`        | Head's full name.              |
| `email`       | `String`        | Login email for the new head.  |
| `schoolName`  | `String`        | Proposed school name.          |
| `schoolCode`  | `String`        | Proposed school code.          |
| `requestedAt` | `LocalDateTime` |                                |
| `status`      | `AccountStatus` | `PENDING`, `APPROVED`, `REJECTED`. |

## teacherApprovals

Id prefix: `tap` · Firestore: `teacherApprovals`

A teacher's request to join an existing school. Approved → teacher account +
optional classes; rejected → archived.

| Field         | Type            | Notes                                     |
|---------------|-----------------|-------------------------------------------|
| `id`          | `String`        |                                           |
| `uid`         | `String`        | Requesting user id.                       |
| `name`        | `String`        |                                           |
| `email`       | `String`        |                                           |
| `schoolCode`  | `String`        | Target school.                            |
| `requestedAt` | `LocalDateTime` |                                           |
| `status`      | `AccountStatus` | `PENDING`, `APPROVED`, `REJECTED`.        |

## students

Id prefix: `stu` · Firestore: `students`

| Field              | Type              | Notes                                                |
|--------------------|-------------------|------------------------------------------------------|
| `id`               | `String`          |                                                      |
| `name`             | `String`          |                                                      |
| `admissionNumber`  | `String`          | Unique per school.                                   |
| `classId`          | `String`          |                                                      |
| `schoolId`         | `String`          |                                                      |
| `parentName`       | `String`          |                                                      |
| `parentPhone`      | `String`          |                                                      |
| `enrollmentStatus` | `EnrollmentStatus`| `PENDING`, `ACTIVE`, `REJECTED`. Only active students appear in marks entry, attendance, report cards; pending students await the head's decision in the students directory. |
| `parentToken`      | `String`          | Unguessable key minted at enrollment; the parent's only credential for the read-only parent view. Two random base36 segments separated by `-`. |
| `teacherId`        | `String`          | Carried over from the reference app's teacher-proposal flow (set when a teacher proposes the student, null when added by the head). Reserved in this build: the console enrolls students as head-created, so the field stays null. |

## classes

Id prefix: `cls` · Firestore: `classes`

| Field         | Type             | Notes                                                    |
|---------------|------------------|----------------------------------------------------------|
| `id`          | `String`         |                                                          |
| `name`        | `String`         | e.g. "Grade 1".                                          |
| `stream`      | `String`         | e.g. "East".                                             |
| `abbreviation`| `String`         | Short code, e.g. "G1E".                                  |
| `schoolId`    | `String`         |                                                          |
| `teacherIds`  | `List<String>`   | Class teachers assigned to the class.                    |
| `subjectIds`  | `List<String>`   | Subjects taught in this class.                           |
| `status`      | `ProposalStatus` | Classes proposed by teachers start `PENDING`; the head approves or rejects. |

## subjects

Id prefix: `sub` · Firestore: `subjects` — the `learningAreaId` field on
assessments (the reference app's domain name for "subject") holds a subject
id.

| Field         | Type          | Notes            |
|---------------|---------------|------------------|
| `id`          | `String`      |                  |
| `name`        | `String`      |                  |
| `abbreviation`| `String`      |                  |
| `schoolId`    | `String`      |                  |
| `status`      | `ProposalStatus`| Subjects proposed by teachers start `PENDING`; the head approves or rejects. |

## terms

Id prefix: `trm` · Firestore: `terms`

| Field          | Type            | Notes                                    |
|----------------|-----------------|------------------------------------------|
| `id`           | `String`        |                                          |
| `name`         | `String`        | e.g. "Term 1".                           |
| `academicYear` | `String`        | e.g. "2026".                             |
| `startDate`    | `LocalDate`     |                                          |
| `endDate`      | `LocalDate`     |                                          |
| `active`       | `boolean`       | Enforced unique: a school has at most one active term. |
| `schoolId`     | `String`        |                                          |

## examTypes

Id prefix: `exm` · Firestore: `examTypes`

| Field      | Type     | Notes                                          |
|------------|----------|------------------------------------------------|
| `id`       | `String` |                                                |
| `name`     | `String` | e.g. "CAT 1", "Mid Term".                      |
| `weight`   | `double` | Contribution when combining scores (0–100).    |
| `schoolId` | `String` |                                                |

## gradeDescriptors

Id prefix: `grd` · Firestore: `gradeDescriptors`

Performance-band labels shown on report cards instead of raw averages.

| Field      | Type     | Notes                                     |
|------------|----------|-------------------------------------------|
| `id`       | `String` |                                           |
| `name`     | `String` | e.g. "Exceeding Expectation".             |
| `minScore` | `double` | Inclusive lower bound of the band.        |
| `maxScore` | `double` | Inclusive upper bound.                    |
| `schoolId` | `String` |                                           |

Bands are expected not to overlap; descriptor resolution picks the first band
whose `[min, max]` range contains the score.

## assessments

Id prefix: `asm` · Firestore: `assessments`

One mark entered by a teacher for one student in one subject, exam series and
term. **Uniqueness rule (upsert):** at most one assessment per
`(studentId, learningAreaId, examTypeId, termId)` — re-entering a mark updates
the existing document.

| Field            | Type     | Notes                                   |
|------------------|----------|-----------------------------------------|
| `id`             | `String` |                                         |
| `studentId`      | `String` |                                         |
| `schoolId`       | `String` |                                         |
| `learningAreaId` | `String` | Subject id.                             |
| `examTypeId`     | `String` |                                         |
| `termId`         | `String` |                                         |
| `rawScore`       | `double` | 0–100.                                  |
| `descriptor`     | `String` | Auto-resolved from the school's grade descriptors at save time. |
| `teacherId`      | `String` | Entering teacher, for audit.            |

## attendance

Id prefix: `att` · Firestore: `attendance`

**Uniqueness rule (upsert):** at most one record per `(studentId, date)` —
re-marking a student for a date updates the existing document.

| Field      | Type              | Notes                       |
|------------|-------------------|-----------------------------|
| `id`       | `String`          |                             |
| `studentId`| `String`          |                             |
| `classId`  | `String`          |                             |
| `date`     | `LocalDate`       |                             |
| `status`   | `AttendanceStatus`| `PRESENT`, `ABSENT`, `LATE`.|
| `teacherId`| `String`          | Marking teacher.            |

## timetableSlots

Id prefix: `tts` · Firestore: `timetableSlots`

| Field         | Type            | Notes                                                |
|---------------|-----------------|------------------------------------------------------|
| `id`          | `String`        |                                                      |
| `teacherId`   | `String`        | Proposing teacher.                                   |
| `schoolId`    | `String`        |                                                      |
| `classId`     | `String`        |                                                      |
| `subjectId`   | `String`        |                                                      |
| `day`         | `DayOfWeek`     | `MONDAY`..`FRIDAY` (CBC learning weeks are Monday–Friday). |
| `period`      | `int`           | 1-based index into the school's period list.         |
| `status`      | `ProposalStatus`| `PENDING`, `APPROVED`, `REJECTED`.                   |
| `requestedAt` | `LocalDateTime` |                                                      |

Lifecycle: teacher proposes → head approves or rejects → head finalises the
whole timetable, after which no further proposals are accepted
(`School.timetableFinalized`).

## calendarEvents

Id prefix: `evt` · Firestore: `calendarEvents`

School calendar events: learning weeks, half-term breaks, holidays, parents'
days. Holidays are excluded from attendance-rate analytics.

| Field       | Type        | Notes                                          |
|-------------|-------------|------------------------------------------------|
| `id`        | `String`    |                                                |
| `schoolId`  | `String`    |                                                |
| `title`     | `String`    |                                                |
| `startDate` | `LocalDate` | Inclusive.                                     |
| `endDate`   | `LocalDate` | Inclusive in the model; the ICS export emits it exclusive (`endDate + 1 day`), per RFC 5545. |
| `holiday`   | `boolean`   | True → excluded from attendance analytics.     |

## tasks

Id prefix: `tsk` · Firestore: `dutyTasks` (field `tasks` in `Repositories`)

Teacher duty tasks (school-assigned or personal).

| Field        | Type            | Notes                               |
|--------------|-----------------|-------------------------------------|
| `id`         | `String`        |                                     |
| `title`      | `String`        |                                     |
| `description`| `String`        |                                     |
| `dueDate`    | `LocalDate`     |                                     |
| `assignedTo` | `String`        | Teacher user id.                    |
| `schoolId`   | `String`        |                                     |
| `completed`  | `boolean`       |                                     |
| `createdAt`  | `LocalDateTime` |                                     |

## comments

Id prefix: `cmt` · Firestore: `overallComments` (field `comments` in
`Repositories`)

A head-teacher's overall remark on a student, printed on the report card.

| Field       | Type            | Notes                                   |
|-------------|-----------------|-----------------------------------------|
| `id`        | `String`        |                                         |
| `studentId` | `String`        |                                         |
| `comment`   | `String`        |                                         |
| `updatedAt` | `LocalDateTime` |                                         |

## notifications

Id prefix: `ntf` · Firestore: `notifications`

| Field         | Type            | Notes                                          |
|---------------|-----------------|------------------------------------------------|
| `id`          | `String`        |                                                |
| `recipientId` | `String`        | User id of the recipient.                      |
| `message`     | `String`        |                                                |
| `type`        | `String`        | Free-form category, e.g. "info", "warning".    |
| `createdAt`   | `LocalDateTime` |                                                |

## headAudit

Id prefix: `aud` · Firestore: *no counterpart* — the head dashboard's audit
trail is an addition of this rewrite; under Firestore it could live as a
subcollection of the school or head document.

Audit trail of school-head actions.

| Field              | Type            | Notes |
|--------------------|-----------------|-------|
| `id`               | `String`        |       |
| `headId`           | `String`        |       |
| `actionDescription`| `String`        |       |
| `at`               | `LocalDateTime` |       |

## How the two storage models map

| Offline build (`InMemoryRepository`)   | Reference web app (Firestore)                 |
|----------------------------------------|-----------------------------------------------|
| `id` field, assigned on save           | Firestore document id                         |
| `<prefix>-<n>` generated ids            | Auto-generated document ids                   |
| `schoolCode` as `School.id`            | School code as document id (same convention)  |
| Java object in a `LinkedHashMap`       | JSON document in a collection                 |
| `findBy(Predicate<T>)`                 | `.where(...)` query                           |
| `save(entity)`                         | `.set(doc)` (upsert)                          |

A Firestore adapter implements `Repository<T>` against one collection per
entity, translating `findBy` filters into where-clauses; the rest of the
codebase — services, UI, exports — does not change. See
[developer-guide.md](developer-guide.md) for the step-by-step recipe.
