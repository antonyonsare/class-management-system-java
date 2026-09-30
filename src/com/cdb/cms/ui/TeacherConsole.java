package com.cdb.cms.ui;

import com.cdb.cms.model.Assessment;
import com.cdb.cms.model.AttendanceStatus;
import com.cdb.cms.model.ClassRoom;
import com.cdb.cms.model.DayOfWeek;
import com.cdb.cms.model.DutyTask;
import com.cdb.cms.model.ExamType;
import com.cdb.cms.model.Notification;
import com.cdb.cms.model.ProposalStatus;
import com.cdb.cms.model.School;
import com.cdb.cms.model.Student;
import com.cdb.cms.model.Subject;
import com.cdb.cms.model.Term;
import com.cdb.cms.model.TimetableSlot;
import com.cdb.cms.model.User;
import com.cdb.cms.service.ServiceException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Teacher console; mirrors teacher.js. */
public class TeacherConsole {

    private final App app;

    public TeacherConsole(App app) {
        this.app = app;
    }

    public void run(User teacher) {
        boolean in = true;
        while (in) {
            switch (Console.menu("Teacher console (" + teacher.getName() + ")",
                    "My classes",
                    "Enter marks",
                    "Take attendance",
                    "Propose timetable slot",
                    "My slot proposals",
                    "Propose a class or subject",
                    "My tasks",
                    "Notifications")) {
                case 1 -> myClasses(teacher);
                case 2 -> enterMarks(teacher);
                case 3 -> takeAttendance(teacher);
                case 4 -> proposeSlot(teacher);
                case 5 -> mySlotProposals(teacher);
                case 6 -> proposeClassOrSubject(teacher);
                case 7 -> tasks(teacher);
                case 8 -> notifications(teacher);
                default -> in = false;
            }
        }
    }

    // ---- classes & students -------------------------------------------------

    private void myClasses(User teacher) {
        List<ClassRoom> mine = app.teacherService().myClasses(teacher.getId());
        if (mine.isEmpty()) {
            Console.println("You are not assigned to any class yet. Ask the school head to assign you.");
        } else {
            List<String[]> rows = new ArrayList<>();
            for (ClassRoom c : mine) {
                rows.add(new String[]{c.getName(), nullToEmpty(c.getStream()),
                        nullToEmpty(c.getAbbreviation()),
                        String.valueOf(c.getTeacherIds() == null ? 0 : c.getTeacherIds().size()),
                        String.valueOf(c.getSubjectIds() == null ? 0 : c.getSubjectIds().size())});
            }
            Console.table(new String[]{"Class", "Stream", "Abbr", "Teachers", "Subjects"}, rows);
        }
        Console.pause();
    }

    private ClassRoom pickClass(User teacher) {
        List<ClassRoom> mine = app.teacherService().myClasses(teacher.getId());
        if (mine.isEmpty()) {
            Console.println("You are not assigned to any class yet.");
            return null;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < mine.size(); i++) {
            ClassRoom c = mine.get(i);
            rows.add(new String[]{String.valueOf(i + 1), c.getName(), nullToEmpty(c.getStream()),
                    String.valueOf(app.teacherService().studentsOf(c.getId()).size())});
        }
        Console.table(new String[]{"#", "Class", "Stream", "Students"}, rows);
        int pick = Console.readInt("Pick a class (0 = back): ", 0, mine.size());
        return pick == 0 ? null : mine.get(pick - 1);
    }

    private Subject pickSubject(User teacher) {
        List<Subject> subjects = app.teacherService().subjectsTaught(teacher.getId(), teacher.getSchoolId());
        if (subjects.isEmpty()) {
            Console.println("No approved subjects in this school yet.");
            return null;
        }
        subjects.sort(Comparator.comparing(Subject::getName));
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < subjects.size(); i++) {
            Subject s = subjects.get(i);
            rows.add(new String[]{String.valueOf(i + 1), s.getName(), nullToEmpty(s.getAbbreviation())});
        }
        Console.table(new String[]{"#", "Subject", "Abbr"}, rows);
        int pick = Console.readInt("Pick a subject (0 = back): ", 0, subjects.size());
        return pick == 0 ? null : subjects.get(pick - 1);
    }

    // ---- marks -----------------------------------------------------------------

    private void enterMarks(User teacher) {
        ClassRoom cls = pickClass(teacher);
        if (cls == null) {
            return;
        }
        List<Student> students = app.teacherService().studentsOf(cls.getId());
        if (students.isEmpty()) {
            Console.println("No active students in " + cls.getName() + ".");
            Console.pause();
            return;
        }
        Subject subject = pickSubject(teacher);
        if (subject == null) {
            return;
        }
        List<ExamType> exams = app.repos().examTypes.findBy(e -> teacher.getSchoolId().equals(e.getSchoolId()));
        if (exams.isEmpty()) {
            Console.println("No exam types configured yet — ask the school head to add some.");
            Console.pause();
            return;
        }
        List<String[]> examRows = new ArrayList<>();
        for (int i = 0; i < exams.size(); i++) {
            examRows.add(new String[]{String.valueOf(i + 1), exams.get(i).getName(),
                    String.valueOf(exams.get(i).getWeight())});
        }
        Console.table(new String[]{"#", "Exam", "Weight"}, examRows);
        int examPick = Console.readInt("Pick an exam (0 = back): ", 0, exams.size());
        if (examPick == 0) {
            return;
        }
        ExamType exam = exams.get(examPick - 1);
        Term term = app.teacherService().activeTerm(teacher.getSchoolId());
        if (term == null) {
            Console.println("No active term — ask the school head to open a term first.");
            Console.pause();
            return;
        }
        Console.println("Entering " + subject.getName() + " / " + exam.getName()
                + " for " + term.getName() + " " + term.getAcademicYear()
                + "  (blank score = skip student)");
        int entered = 0;
        for (Student s : students) {
            String raw = Console.readLineOrEmpty(String.format("%-28s score: ", s.getName()));
            if (raw.isEmpty()) {
                continue;
            }
            try {
                double score = Double.parseDouble(raw);
                Assessment a = app.teacherService().enterMark(teacher.getId(), s.getId(),
                        teacher.getSchoolId(), subject.getId(), exam.getId(), term.getId(), score);
                Console.println("   saved: " + trim(score) + " -> " + a.getDescriptor());
                entered++;
            } catch (NumberFormatException ex) {
                Console.println("   (not a number — skipped)");
            } catch (ServiceException ex) {
                Console.println("   " + ex.getMessage());
            }
        }
        Console.println(entered + " mark(s) saved.");
        Console.pause();
    }

    // ---- attendance ------------------------------------------------------------

    private void takeAttendance(User teacher) {
        ClassRoom cls = pickClass(teacher);
        if (cls == null) {
            return;
        }
        List<Student> students = app.teacherService().studentsOf(cls.getId());
        if (students.isEmpty()) {
            Console.println("No active students in " + cls.getName() + ".");
            Console.pause();
            return;
        }
        LocalDate date = Console.readDateOrToday("Attendance date");
        Map<String, AttendanceStatus> statuses = new LinkedHashMap<>();
        Console.println("Mark each student: 1 = Present, 2 = Absent, 3 = Late");
        for (Student s : students) {
            int choice = Console.readInt(String.format("%-28s ", s.getName()) + ": ", 1, 3);
            statuses.put(s.getId(), choice == 1 ? AttendanceStatus.PRESENT
                    : choice == 2 ? AttendanceStatus.ABSENT : AttendanceStatus.LATE);
        }
        try {
            int saved = app.teacherService().saveAttendance(teacher.getId(), cls.getId(), date, statuses);
            Console.println("Saved " + saved + " attendance row(s) for " + date + ".");
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    // ---- timetable & proposals -----------------------------------------------------

    private void proposeSlot(User teacher) {
        School school = app.repos().schools.findById(teacher.getSchoolId()).orElse(null);
        if (school == null) {
            Console.println("School record not found.");
            Console.pause();
            return;
        }
        if (school.isTimetableFinalized()) {
            Console.println("The timetable has been finalised by the school head; no new slots can be proposed.");
            Console.pause();
            return;
        }
        ClassRoom cls = pickClass(teacher);
        if (cls == null) {
            return;
        }
        Subject subject = pickSubject(teacher);
        if (subject == null) {
            return;
        }
        int day = Console.readInt("Day (1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri): ", 1, 5);
        int periods = school.getTimetablePeriods().size();
        int period = Console.readInt("Period (1-" + periods + "): ", 1, periods);
        try {
            app.teacherService().proposeSlot(teacher.getId(), teacher.getSchoolId(), cls.getId(),
                    subject.getId(), DayOfWeek.fromMenuChoice(day), period);
            Console.println("Proposed " + subject.getName() + " for " + cls.getName()
                    + " on day " + day + " period " + period + " — awaiting school head approval.");
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void mySlotProposals(User teacher) {
        List<TimetableSlot> slots = app.teacherService().mySlotProposals(teacher.getId());
        slots.sort(Comparator.comparing(TimetableSlot::getDay).thenComparingInt(TimetableSlot::getPeriod));
        if (slots.isEmpty()) {
            Console.println("You have not proposed any timetable slots.");
        } else {
            List<String[]> rows = new ArrayList<>();
            for (TimetableSlot s : slots) {
                rows.add(new String[]{s.getDay().name(), String.valueOf(s.getPeriod()),
                        className(s.getClassId()), subjectName(s.getSubjectId()),
                        s.getStatus().name(),
                        s.getRequestedAt() == null ? "" : s.getRequestedAt().toLocalDate().toString()});
            }
            Console.table(new String[]{"Day", "Period", "Class", "Subject", "Status", "Requested"}, rows);
            if (slots.stream().anyMatch(s -> s.getStatus() == ProposalStatus.PENDING)) {
                Console.println("(pending slots are awaiting the school head's decision)");
            }
        }
        Console.pause();
    }

    private void proposeClassOrSubject(User teacher) {
        int kind = Console.menu("Propose", "A new class", "A new subject");
        try {
            if (kind == 1) {
                String name = Console.readLine("Class name (e.g. Grade 3): ");
                String stream = Console.readLineOrEmpty("Stream (optional): ");
                String abbr = Console.readLineOrEmpty("Abbreviation (optional): ");
                app.teacherService().proposeClass(teacher.getId(), teacher.getSchoolId(), name, stream, abbr);
                Console.println("Class proposal submitted — awaiting school head approval.");
            } else if (kind == 2) {
                String name = Console.readLine("Subject name: ");
                String abbr = Console.readLineOrEmpty("Abbreviation (optional): ");
                app.teacherService().proposeSubject(teacher.getId(), teacher.getSchoolId(), name, abbr);
                Console.println("Subject proposal submitted — awaiting school head approval.");
            }
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    // ---- tasks & notifications -----------------------------------------------------

    private void tasks(User teacher) {
        List<DutyTask> tasks = app.teacherService().myTasks(teacher.getId());
        tasks.sort(Comparator.comparing((DutyTask t) -> t.isCompleted())
                .thenComparing(DutyTask::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())));
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            DutyTask t = tasks.get(i);
            rows.add(new String[]{String.valueOf(i + 1), t.getTitle(),
                    t.getDueDate() == null ? "-" : t.getDueDate().toString(), badge(t)});
        }
        Console.table(new String[]{"#", "Task", "Due", "Status"}, rows);
        if (tasks.isEmpty()) {
            Console.println("No tasks.");
        }
        int pick = Console.readInt("Mark which task complete? (0 = none): ", 0, tasks.size());
        if (pick > 0) {
            DutyTask t = tasks.get(pick - 1);
            try {
                app.teacherService().completeTask(t.getId());
                Console.println("Marked \"" + t.getTitle() + "\" complete.");
            } catch (ServiceException ex) {
                Console.println("Failed: " + ex.getMessage());
            }
        }
        if (Console.menu("Tasks", "Add a personal task") == 1) {
            String title = Console.readLine("Title: ");
            String description = Console.readLineOrEmpty("Description (optional): ");
            LocalDate due = readOptionalDate("Due date");
            try {
                app.teacherService().addPersonalTask(teacher.getSchoolId(), teacher.getId(), title, description, due);
                Console.println("Personal task added.");
            } catch (ServiceException ex) {
                Console.println("Failed: " + ex.getMessage());
            }
        }
        Console.pause();
    }

    private void notifications(User teacher) {
        List<Notification> notes = app.teacherService().myNotifications(teacher.getId());
        notes.sort(Comparator.comparing(Notification::getCreatedAt).reversed());
        if (notes.isEmpty()) {
            Console.println("No notifications.");
        } else {
            List<String[]> rows = new ArrayList<>();
            for (Notification n : notes) {
                rows.add(new String[]{n.getCreatedAt() == null ? "" : n.getCreatedAt().toString(),
                        n.getType(), n.getMessage()});
            }
            Console.table(new String[]{"When", "Type", "Message"}, rows);
        }
        Console.pause();
    }

    // ---- helpers ---------------------------------------------------------------------

    private String badge(DutyTask t) {
        if (t.isCompleted()) {
            return "Done";
        }
        if (t.getDueDate() == null) {
            return "Open";
        }
        LocalDate today = LocalDate.now();
        if (t.getDueDate().isBefore(today)) {
            return "OVERDUE";
        }
        if (!t.getDueDate().isAfter(today.plusDays(3))) {
            return "Due soon";
        }
        return "Open";
    }

    private LocalDate readOptionalDate(String prompt) {
        String raw = Console.readLineOrEmpty(prompt + " (yyyy-mm-dd, Enter = none): ");
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (java.time.format.DateTimeParseException e) {
            Console.println("  (unparseable date, using none)");
            return null;
        }
    }

    private String className(String classId) {
        return classId == null ? "-"
                : app.repos().classes.findById(classId).map(ClassRoom::getName).orElse(classId);
    }

    private String subjectName(String subjectId) {
        return subjectId == null ? "-"
                : app.repos().subjects.findById(subjectId).map(Subject::getName).orElse(subjectId);
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format("%.2f", d);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
