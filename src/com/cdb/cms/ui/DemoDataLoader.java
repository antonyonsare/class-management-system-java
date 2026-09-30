package com.cdb.cms.ui;

import com.cdb.cms.model.AccountStatus;
import com.cdb.cms.model.AttendanceStatus;
import com.cdb.cms.model.ClassRoom;
import com.cdb.cms.model.ExamType;
import com.cdb.cms.model.School;
import com.cdb.cms.model.Student;
import com.cdb.cms.model.Subject;
import com.cdb.cms.model.Term;
import com.cdb.cms.model.TimetableSlot;
import com.cdb.cms.model.User;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preloads one complete demo school so every role can be exercised without
 * manual data entry, then prints the demo credentials (including a parent
 * token) to the console. Deterministic: the same data is created on every
 * run, except generated school codes and parent tokens which are random.
 */
public class DemoDataLoader {

    private final App app;

    public DemoDataLoader(App app) {
        this.app = app;
    }

    public void load() {
        // Superadmin (the bootstrap the real first run asks for interactively).
        app.auth().bootstrapSuperadmin("Platform Superadmin", "admin@cdb.test", "admin123".toCharArray());

        // A school head registers a new school; the superadmin approves her,
        // which creates the school and generates its join code.
        app.auth().registerHead("Grace Wanjiku", "head@greenhill.test", "head123".toCharArray(),
                "Greenhill Academy", null);
        var headApproval = app.repos().headApprovals.findBy(a -> a.getStatus() == AccountStatus.PENDING).get(0);
        app.superAdmin().approveHead(headApproval.getId());
        User head = app.repos().users.findFirst(u -> "head@greenhill.test".equals(u.getEmail())).orElseThrow();
        String schoolCode = head.getSchoolId();
        School school = app.repos().schools.findById(schoolCode).orElseThrow();

        // Two teachers register with the school code and get approved.
        app.auth().registerTeacher("Peter Otieno", "peter@greenhill.test", "teach123".toCharArray(), schoolCode);
        app.auth().registerTeacher("Mary Achieng", "mary@greenhill.test", "teach123".toCharArray(), schoolCode);
        for (var t : app.superAdmin().pendingTeachers()) {
            app.superAdmin().approveTeacher(t.getId());
        }
        List<User> teachers = app.headService().teachers(schoolCode);

        // Structure: classes, subjects, terms, exam types, grade descriptors.
        ClassRoom grade1 = app.headService().addClass(schoolCode, "Grade 1", "A", "G1A", head.getId());
        ClassRoom grade2 = app.headService().addClass(schoolCode, "Grade 2", "A", "G2A", head.getId());
        Subject maths = app.headService().addSubject(schoolCode, "Mathematics", "MTH", head.getId());
        Subject english = app.headService().addSubject(schoolCode, "English", "ENG", head.getId());
        Subject science = app.headService().addSubject(schoolCode, "Science", "SCI", head.getId());
        Term term1 = app.headService().addTerm(schoolCode, "Term 1", "2026",
                LocalDate.of(2026, 1, 6), LocalDate.of(2026, 3, 27), head.getId());
        ExamType cat1 = app.headService().addExamType(schoolCode, "CAT 1", 30, head.getId());
        ExamType midTerm = app.headService().addExamType(schoolCode, "Mid Term", 70, head.getId());
        app.headService().addGradeDescriptor(schoolCode, "Exceeding Expectation", 80, 100, head.getId());
        app.headService().addGradeDescriptor(schoolCode, "Meeting Expectation", 60, 79, head.getId());
        app.headService().addGradeDescriptor(schoolCode, "Approaching Expectation", 40, 59, head.getId());
        app.headService().addGradeDescriptor(schoolCode, "Below Expectation", 0, 39, head.getId());

        app.headService().setClassTeachers(grade1.getId(), List.of(teachers.get(0).getId()), head.getId());
        app.headService().setClassTeachers(grade2.getId(), List.of(teachers.get(1).getId()), head.getId());
        app.headService().setClassSubjects(grade1.getId(),
                List.of(maths.getId(), english.getId(), science.getId()), head.getId());
        app.headService().setClassSubjects(grade2.getId(),
                List.of(maths.getId(), english.getId()), head.getId());

        // Students (direct enrollment mints the parent tokens).
        String[][] roster = {
                {"Amina Baraka", "1"}, {"Brian Kiprop", "1"}, {"Cynthia Njeri", "1"},
                {"David Mwangi", "1"}, {"Esther Wambui", "2"}, {"Felix Omondi", "2"},
        };
        ClassRoom[] classes = {grade1, grade1, grade1, grade1, grade2, grade2};
        Student[] students = new Student[roster.length];
        for (int i = 0; i < roster.length; i++) {
            students[i] = app.headService().enrollStudent(schoolCode, roster[i][0], null,
                    classes[i].getId(), "Parent of " + roster[i][0], "0700" + String.format("%06d", 100000 + i * 137),
                    head.getId());
        }

        // Marks: every student gets a deterministic spread across subjects/exams.
        Subject[][] subjectsByStudent = {
                {maths, english, science}, {maths, english, science}, {maths, english, science},
                {maths, english, science}, {maths, english}, {maths, english},
        };
        ExamType[] exams = {cat1, midTerm};
        for (int i = 0; i < students.length; i++) {
            int s = 48 + ((i * 17) % 46); // 48..93, spreads the class across descriptors
            for (Subject sub : subjectsByStudent[i]) {
                for (ExamType ex : exams) {
                    double score = Math.min(100, Math.max(0, s + (sub.getId().hashCode() % 7) - 3 + (ex == cat1 ? -2 : 2)));
                    app.teacherService().enterMark(teachers.get(i < 4 ? 0 : 1).getId(),
                            students[i].getId(), schoolCode, sub.getId(), ex.getId(),
                            term1.getId(), Math.round(score));
                }
            }
        }

        // One day of attendance per class.
        LocalDate day = LocalDate.of(2026, 2, 10);
        saveAttendance(teachers.get(0).getId(), grade1.getId(), day,
                new String[]{"Amina Baraka", "Brian Kiprop", "Cynthia Njeri"},
                new String[]{"David Mwangi"});
        saveAttendance(teachers.get(1).getId(), grade2.getId(), day,
                new String[]{"Esther Wambui"}, new String[]{"Felix Omondi"});

        // Timetable: teachers propose slots, the head approves some.
        TimetableSlot s1 = app.teacherService().proposeSlot(teachers.get(0).getId(), schoolCode,
                grade1.getId(), maths.getId(), com.cdb.cms.model.DayOfWeek.MONDAY, 1);
        TimetableSlot s2 = app.teacherService().proposeSlot(teachers.get(0).getId(), schoolCode,
                grade1.getId(), english.getId(), com.cdb.cms.model.DayOfWeek.MONDAY, 2);
        TimetableSlot s3 = app.teacherService().proposeSlot(teachers.get(1).getId(), schoolCode,
                grade2.getId(), maths.getId(), com.cdb.cms.model.DayOfWeek.TUESDAY, 1);
        TimetableSlot s4 = app.teacherService().proposeSlot(teachers.get(1).getId(), schoolCode,
                grade2.getId(), english.getId(), com.cdb.cms.model.DayOfWeek.TUESDAY, 2);
        app.headService().approveSlot(s1.getId(), head.getId());
        app.headService().approveSlot(s2.getId(), head.getId());
        app.headService().approveSlot(s3.getId(), head.getId());
        app.headService().rejectSlot(s4.getId(), head.getId(), "Period 2 on Tuesday is reserved for PE.");

        // A task, an event, a comment.
        app.headService().assignTask(schoolCode, "Prepare Term 1 report cards",
                "Draft remarks for every Grade 1 student before Friday.",
                LocalDate.now().plusDays(3), teachers.get(0).getId(), head.getId());
        app.headService().assignTask(schoolCode, "Update class registers",
                "Confirm all February attendance is captured.",
                LocalDate.now().minusDays(1), teachers.get(0).getId(), head.getId());
        app.headService().addEvent(schoolCode, "Half-term break", LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 20), true, head.getId());
        app.headService().addEvent(schoolCode, "Parents' day", LocalDate.of(2026, 3, 13), null,
                false, head.getId());
        app.headService().setOverallComment(students[0].getId(),
                "Amina shows excellent focus and helps her peers.", head.getId());

        printSummary(school, teachers, students);
    }

    private void saveAttendance(String teacherId, String classId, LocalDate day,
                                String[] present, String[] absent) {
        List<String> presentNames = List.of(present);
        List<String> absentNames = List.of(absent);
        Map<String, AttendanceStatus> rows = new LinkedHashMap<>();
        for (Student s : app.teacherService().studentsOf(classId)) {
            if (absentNames.contains(s.getName())) {
                rows.put(s.getId(), AttendanceStatus.ABSENT);
            } else if (presentNames.contains(s.getName())) {
                rows.put(s.getId(), AttendanceStatus.PRESENT);
            } else {
                rows.put(s.getId(), AttendanceStatus.LATE);
            }
        }
        app.teacherService().saveAttendance(teacherId, classId, day, rows);
    }

    private void printSummary(School school, List<User> teachers, Student[] students) {
        Console.println();
        Console.println("================ DEMO DATA LOADED ================");
        Console.println(" School : " + school.getName() + "  (code " + school.getSchoolCode() + ")");
        Console.println("---------------------------------------------------");
        Console.println(" ROLE      | LOGIN                    | PASSWORD");
        Console.println(" superadmin| admin@cdb.test           | admin123");
        Console.println(" head      | head@greenhill.test      | head123");
        Console.println(" teacher   | peter@greenhill.test     | teach123");
        Console.println(" teacher   | mary@greenhill.test      | teach123");
        Console.println("---------------------------------------------------");
        Console.println(" Sample parent token (Amina Baraka): " + students[0].getParentToken());
        Console.println("===================================================");
        Console.println();
    }
}
