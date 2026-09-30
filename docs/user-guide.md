# User guide

A walkthrough of the application from each role's perspective. The console
prompts are numbered menus: type the number of an item and press Enter;
`0` always means *back* or *log out*.

## Starting the application

```bash
java -cp out com.cdb.cms.Main --demo     # with sample data
java -cp out com.cdb.cms.Main             # empty (production-style) start
```

The `--demo` flag loads a deterministic sample school — Greenhill Academy —
so every feature can be explored immediately. See the
[README](../README.md#demo-credentials) for the demo credentials.

The welcome screen offers four doors:

| # | Option                              | Where it leads                                    |
|---|-------------------------------------|---------------------------------------------------|
| 1 | Log in                              | Email + password → role console.                  |
| 2 | Register as a school head           | Found a new school, or request to join one by code. The request waits for superadmin approval — you cannot log in until approved. |
| 3 | Register as a teacher               | Request to join a school by code; the school head approves. |
| 4 | Parent access (student token)       | No account needed — the token printed at enrollment is the only credential. |
| 0 | Exit                                |                                                   |

Passwords are hidden when the terminal supports it; typed input is echoed in
plain terminals (the fallback path). Ending the input stream (Ctrl-D / Ctrl-Z)
shuts the application down cleanly from any prompt.

## Superadmin

The superadmin runs the whole platform: schools exist because this account
approves them. Sign in with `admin@cdb.test` / `admin123` (demo).

**Pending head registrations / Pending teacher registrations** — lists
requests newest-first. Approving a founding-head request creates the school
and its join code; approving a joining head or teacher activates the account
under the named school. Rejections are kept for audit.

**Schools directory** — every school with its code, head, student/teacher/
class counts and creation date.

**Send reminder to a school head** — picks a school and notifies its head
(e.g. "approve your pending teacher requests"). The notification appears in
the head's (and teacher's) notification list.

**Platform statistics** — totals across the platform: schools, students,
teachers, classes, subjects, assessments.

**Export schools CSV** — writes `schools.csv` (RFC 4180: quoted fields,
CRLF rows) into the current directory: one row per school with head name,
counts and creation date.

## School head

The head owns one school's setup. Demo: `head@greenhill.test` / `head123`
(Greenhill Academy).

1. **Teacher approvals** — approve or reject teacher join requests.
2. **Enroll student** — creates the student under a class. On success the
   system prints the **parent token**, e.g.
   `(Share this token with the parent — it is their only way in.)`
   Copy it immediately; it is shown only here.
3. **Students directory** — all students with admission numbers, classes and
   enrollment status. Any student with status PENDING (the teacher-proposal
   flow carried over from the reference app) is listed for the head to
   approve or reject; enrollment by the head is direct, so new students are
   ACTIVE immediately.
4. **Classes** — list, add, review class proposals from teachers, assign
   teachers to a class, assign subjects to a class.
5. **Subjects** — list, add, review subject proposals from teachers.
6. **Terms, exams & grading** — list terms (at most one active per school);
   adding a term makes it the active one. Add exam types with a weight used
   when combining scores, and the grade descriptors (score bands) that become
   report-card labels.
7. **Timetable** — review pending slot proposals (approve/reject each), view
   the approved timetable grid, and **finalise**. Finalising is one-way: the
   menu gains a `(FINALISED)` marker and teachers can no longer propose slots.
8. **Tasks** — assign duty tasks to teachers with due dates, or list every
   school task and its completion state.
9. **Overall comments** — a free-text remark per student, printed on the
   report card under *Class Teacher's / Head Teacher's remarks*.
10. **Calendar events** — school events (half-term, parents' day, holidays).
    Marking an event as a *holiday* excludes its days from attendance-rate
    analytics.
11. **Analytics** — per term: class averages per subject as a matrix, each
    student's weighted average with descriptor, subject performance ranking,
   and per-class top performers vs. students needing support (attendance and
   average, with the gap to the class average).
12. **Dashboard** — attendance summaries, counts and recent audit entries.
13. **Export students CSV** — `students.csv`: name, admission number, class,
    parent contact, status.

## Teacher

Teachers do the daily academic work. Demo: `peter@greenhill.test` /
`teach123` (Grade 1) or `mary@greenhill.test` / `teach123` (Grade 2).

1. **My classes** — the classes assigned to you, with their students.
2. **Enter marks** — pick class → subject → exam. Every active student in the
   class is listed with their current mark; type a score (0–100) or leave
   blank to skip. Each saved mark shows its auto-resolved descriptor, e.g.
   `saved: 65 -> Meeting Expectation`. Re-entering a mark for the same
   student/subject/exam/term updates it rather than duplicating it.
3. **Take attendance** — pick class → date → each student: `1` present,
   `2` absent, `3` late. Re-marking the same student for a date updates the
   record.
4. **Propose timetable slot** — pick class, subject, day and period. The
   proposal waits for the school head's approval. Blocked once the head has
   finalised the timetable.
5. **My slot proposals** — your proposals with their status
   (PENDING / APPROVED / REJECTED).
6. **Propose a class or subject** — suggest a new class or subject to the
   head; approved suggestions appear in the school's setup.
7. **My tasks** — school-assigned and personal tasks, sorted by urgency
   (OVERDUE / Due soon / Open / Done badges). Add personal tasks and mark
   tasks complete.
8. **Notifications** — messages from the head/superadmin, newest first.

## Parent (token access)

From the welcome screen choose **Parent access** and enter the token printed
at enrollment (e.g. the demo run prints
`Sample parent token (Amina Baraka): ...`). No password, no account — the
token is the credential, so treat it like one.

1. **Full report on screen** — the report card: student and school header,
   attendance percentage and average for the active term, per-subject scores
   with descriptors, and the head's overall comment.
2. **Export report card (HTML)** — writes a printable, self-contained HTML
   report card into `exports/` (or a directory you choose) — ready to print
   or send.
3. **Export school calendar (ICS)** — downloads an iCalendar file of the
   school's events for any calendar app.
4. **View weekly timetable** — the approved timetable grid for your child's
   class.
5. **School events** — upcoming events with dates and holiday markers.

## Demo data quick tour

With `--demo` loaded, this sequence touches every role in a few minutes:

1. Log in as `head@greenhill.test` / `head123` → **Analytics** to see the
   pre-entered marks analysed; **Timetable** to approve the pending proposal
   and finalise.
2. Log in as `peter@greenhill.test` / `teach123` → **Enter marks** for CAT 1
   (note the descriptor auto-resolution), **Take attendance** for today,
   **Propose timetable slot** (rejected politely if finalised).
3. Log in as `admin@cdb.test` / `admin123` → **Platform statistics**,
   **Export schools CSV**.
4. From the welcome screen, choose **Parent access** with Amina's printed
   token → **Full report**, then export the HTML report card.
