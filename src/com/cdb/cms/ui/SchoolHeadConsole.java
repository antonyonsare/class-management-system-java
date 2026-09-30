package com.cdb.cms.ui;

import com.cdb.cms.dto.AnalyticsMatrix;
import com.cdb.cms.dto.ClassPerformance;
import com.cdb.cms.dto.DashboardStats;
import com.cdb.cms.dto.MatrixRow;
import com.cdb.cms.model.CalendarEvent;
import com.cdb.cms.model.ClassRoom;
import com.cdb.cms.model.DutyTask;
import com.cdb.cms.model.EnrollmentStatus;
import com.cdb.cms.model.ExamType;
import com.cdb.cms.model.GradeDescriptor;
import com.cdb.cms.model.OverallComment;
import com.cdb.cms.model.Student;
import com.cdb.cms.model.Subject;
import com.cdb.cms.model.TeacherApproval;
import com.cdb.cms.model.Term;
import com.cdb.cms.model.TimetableSlot;
import com.cdb.cms.model.User;
import com.cdb.cms.service.ServiceException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** School head console; mirrors schoolhead.js. */
public class SchoolHeadConsole {

    private final App app;
    private User head;
    private String schoolCode;

    public SchoolHeadConsole(App app) {
        this.app = app;
    }

    public void run(User head) {
        this.head = head;
        this.schoolCode = head.getSchoolId();
        boolean in = true;
        while (in) {
            switch (Console.menu("School head console (" + head.getName() + " - " + schoolCode + ")",
                    "Teacher approvals",
                    "Enroll student",
                    "Students directory",
                    "Classes",
                    "Subjects",
                    "Terms, exams & grading",
                    "Timetable",
                    "Tasks",
                    "Overall comments",
                    "Calendar events",
                    "Analytics",
                    "Dashboard",
                    "Export students CSV")) {
                case 1 -> teacherApprovals();
                case 2 -> enrollStudent();
                case 3 -> studentsDirectory();
                case 4 -> classesMenu();
                case 5 -> subjectsMenu();
                case 6 -> termsExamsGradingMenu();
                case 7 -> timetableMenu();
                case 8 -> tasksMenu();
                case 9 -> overallComments();
                case 10 -> eventsMenu();
                case 11 -> analyticsMenu();
                case 12 -> dashboard();
                case 13 -> exportCsv();
                default -> in = false;
            }
        }
    }

    // ---- teacher approvals -------------------------------------------------

    private void teacherApprovals() {
        List<TeacherApproval> pending = app.headService().pendingTeachers(schoolCode);
        if (pending.isEmpty()) {
            Console.println("No pending teacher registrations.");
            Console.pause();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < pending.size(); i++) {
            TeacherApproval t = pending.get(i);
            rows.add(new String[]{String.valueOf(i + 1), t.getName(), t.getEmail(),
                    t.getRequestedAt() == null ? "" : t.getRequestedAt().toLocalDate().toString()});
        }
        Console.table(new String[]{"#", "Name", "Email", "Requested"}, rows);
        int pick = Console.readInt("Pick a registration (0 = back): ", 0, pending.size());
        if (pick == 0) {
            return;
        }
        TeacherApproval t = pending.get(pick - 1);
        int action = Console.menu("Decision for " + t.getName(), "Approve", "Reject");
        try {
            if (action == 1) {
                app.headService().approveTeacher(t.getId(), head.getId());
                Console.println("Approved — " + t.getName() + " can now log in.");
            } else if (action == 2) {
                String reason = Console.readLineOrEmpty("Reason (optional): ");
                app.headService().rejectTeacher(t.getId(), head.getId(), reason);
                Console.println("Rejected.");
            }
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    // ---- students ------------------------------------------------------------

    private void enrollStudent() {
        String name = Console.readLine("Student name: ");
        String admission = Console.readLineOrEmpty("Admission number (Enter = auto): ");
        String classId = pickClassId();
        String parentName = Console.readLineOrEmpty("Parent name: ");
        String parentPhone = Console.readLineOrEmpty("Parent phone: ");
        try {
            Student s = app.headService().enrollStudent(schoolCode, name, admission,
                    classId, parentName, parentPhone, head.getId());
            Console.println("Enrolled " + s.getName() + " (" + s.getAdmissionNumber() + ").");
            Console.println("Parent access token: " + s.getParentToken());
            Console.println("(Share this token with the parent — it is their only way in.)");
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void studentsDirectory() {
        List<Student> students = app.headService().students(schoolCode);
        students.sort(Comparator.comparing(Student::getName));
        List<String[]> rows = new ArrayList<>();
        for (Student s : students) {
            rows.add(new String[]{s.getAdmissionNumber(), s.getName(), className(s.getClassId()),
                    s.getEnrollmentStatus().name(), nullToEmpty(s.getParentName())});
        }
        Console.table(new String[]{"Adm No", "Name", "Class", "Status", "Parent"}, rows);

        List<Student> pending = students.stream()
                .filter(s -> s.getEnrollmentStatus() == EnrollmentStatus.PENDING).toList();
        if (!pending.isEmpty()) {
            List<String[]> prows = new ArrayList<>();
            for (int i = 0; i < pending.size(); i++) {
                prows.add(new String[]{String.valueOf(i + 1), pending.get(i).getName(),
                        pending.get(i).getAdmissionNumber()});
            }
            Console.table(new String[]{"#", "Pending student", "Adm No"}, prows);
            int pick = Console.readInt("Decide on which? (0 = none): ", 0, pending.size());
            if (pick > 0) {
                Student s = pending.get(pick - 1);
                int action = Console.menu("Decision for " + s.getName(), "Approve", "Reject");
                try {
                    if (action == 1) {
                        app.headService().approveStudent(s.getId(), head.getId());
                        Console.println("Approved.");
                    } else if (action == 2) {
                        app.headService().rejectStudent(s.getId(), head.getId());
                        Console.println("Rejected.");
                    }
                } catch (ServiceException ex) {
                    Console.println("Failed: " + ex.getMessage());
                }
            }
        }
        Console.pause();
    }

    // ---- classes ---------------------------------------------------------------

    private void classesMenu() {
        boolean in = true;
        while (in) {
            switch (Console.menu("Classes",
                    "List classes",
                    "Add a class",
                    "Class proposals (" + app.headService().classProposals(schoolCode).size() + " pending)",
                    "Assign teachers to a class",
                    "Assign subjects to a class")) {
                case 1 -> listClasses();
                case 2 -> addClass();
                case 3 -> classProposals();
                case 4 -> assignTeachers();
                case 5 -> assignSubjects();
                default -> in = false;
            }
        }
    }

    private void listClasses() {
        List<ClassRoom> classes = app.headService().classes(schoolCode);
        classes.sort(Comparator.comparing(ClassRoom::getName));
        List<String[]> rows = new ArrayList<>();
        for (ClassRoom c : classes) {
            rows.add(new String[]{c.getName(), nullToEmpty(c.getStream()), nullToEmpty(c.getAbbreviation()),
                    String.valueOf(c.getTeacherIds() == null ? 0 : c.getTeacherIds().size()),
                    String.valueOf(c.getSubjectIds() == null ? 0 : c.getSubjectIds().size())});
        }
        Console.table(new String[]{"Class", "Stream", "Abbr", "Teachers", "Subjects"}, rows);
        Console.pause();
    }

    private void addClass() {
        String name = Console.readLine("Class name (e.g. Grade 3): ");
        String stream = Console.readLineOrEmpty("Stream (optional): ");
        String abbr = Console.readLineOrEmpty("Abbreviation (optional): ");
        try {
            ClassRoom c = app.headService().addClass(schoolCode, name, stream, abbr, head.getId());
            Console.println("Class \"" + c.getName() + "\" added.");
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void classProposals() {
        List<ClassRoom> proposals = app.headService().classProposals(schoolCode);
        if (proposals.isEmpty()) {
            Console.println("No pending class proposals.");
            Console.pause();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < proposals.size(); i++) {
            ClassRoom c = proposals.get(i);
            rows.add(new String[]{String.valueOf(i + 1), c.getName(),
                    nullToEmpty(c.getStream()), nullToEmpty(c.getAbbreviation())});
        }
        Console.table(new String[]{"#", "Proposed class", "Stream", "Abbr"}, rows);
        int pick = Console.readInt("Pick a proposal (0 = back): ", 0, proposals.size());
        if (pick == 0) {
            return;
        }
        ClassRoom c = proposals.get(pick - 1);
        int action = Console.menu("Decision for " + c.getName(), "Approve", "Reject");
        try {
            if (action == 1) {
                app.headService().approveClassProposal(c.getId(), head.getId());
                Console.println("Approved.");
            } else if (action == 2) {
                app.headService().rejectClassProposal(c.getId(), head.getId());
                Console.println("Rejected.");
            }
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void assignTeachers() {
        ClassRoom cls = pickClassObject();
        if (cls == null) {
            return;
        }
        List<User> teachers = app.headService().teachers(schoolCode);
        teachers.sort(Comparator.comparing(User::getName));
        if (teachers.isEmpty()) {
            Console.println("No approved teachers in this school yet.");
            Console.pause();
            return;
        }
        List<String> current = cls.getTeacherIds() == null ? List.of() : cls.getTeacherIds();
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < teachers.size(); i++) {
            User t = teachers.get(i);
            rows.add(new String[]{String.valueOf(i + 1), t.getName(), t.getEmail(),
                    current.contains(t.getId()) ? "assigned" : ""});
        }
        Console.table(new String[]{"#", "Teacher", "Email", ""}, rows);
        List<Integer> picks = readMulti("Teacher numbers, comma-separated (Enter = none): ", teachers.size());
        List<String> ids = picks.stream().map(i -> teachers.get(i - 1).getId()).toList();
        try {
            app.headService().setClassTeachers(cls.getId(), ids, head.getId());
            Console.println("Teachers of " + cls.getName() + " updated (" + ids.size() + " assigned).");
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void assignSubjects() {
        ClassRoom cls = pickClassObject();
        if (cls == null) {
            return;
        }
        List<Subject> subjects = app.headService().subjects(schoolCode);
        subjects.sort(Comparator.comparing(Subject::getName));
        if (subjects.isEmpty()) {
            Console.println("No approved subjects in this school yet.");
            Console.pause();
            return;
        }
        List<String> current = cls.getSubjectIds() == null ? List.of() : cls.getSubjectIds();
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < subjects.size(); i++) {
            Subject s = subjects.get(i);
            rows.add(new String[]{String.valueOf(i + 1), s.getName(),
                    nullToEmpty(s.getAbbreviation()), current.contains(s.getId()) ? "assigned" : ""});
        }
        Console.table(new String[]{"#", "Subject", "Abbr", ""}, rows);
        List<Integer> picks = readMulti("Subject numbers, comma-separated (Enter = none): ", subjects.size());
        List<String> ids = picks.stream().map(i -> subjects.get(i - 1).getId()).toList();
        try {
            app.headService().setClassSubjects(cls.getId(), ids, head.getId());
            Console.println("Subjects of " + cls.getName() + " updated (" + ids.size() + " assigned).");
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    // ---- subjects ---------------------------------------------------------------

    private void subjectsMenu() {
        boolean in = true;
        while (in) {
            switch (Console.menu("Subjects",
                    "List subjects",
                    "Add a subject",
                    "Subject proposals (" + app.headService().subjectProposals(schoolCode).size() + " pending)")) {
                case 1 -> listSubjects();
                case 2 -> addSubject();
                case 3 -> subjectProposals();
                default -> in = false;
            }
        }
    }

    private void listSubjects() {
        List<Subject> subjects = app.headService().subjects(schoolCode);
        subjects.sort(Comparator.comparing(Subject::getName));
        List<String[]> rows = new ArrayList<>();
        for (Subject s : subjects) {
            rows.add(new String[]{s.getName(), nullToEmpty(s.getAbbreviation())});
        }
        Console.table(new String[]{"Subject", "Abbr"}, rows);
        Console.pause();
    }

    private void addSubject() {
        String name = Console.readLine("Subject name: ");
        String abbr = Console.readLineOrEmpty("Abbreviation (optional): ");
        try {
            Subject s = app.headService().addSubject(schoolCode, name, abbr, head.getId());
            Console.println("Subject \"" + s.getName() + "\" added.");
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void subjectProposals() {
        List<Subject> proposals = app.headService().subjectProposals(schoolCode);
        if (proposals.isEmpty()) {
            Console.println("No pending subject proposals.");
            Console.pause();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < proposals.size(); i++) {
            Subject s = proposals.get(i);
            rows.add(new String[]{String.valueOf(i + 1), s.getName(), nullToEmpty(s.getAbbreviation())});
        }
        Console.table(new String[]{"#", "Proposed subject", "Abbr"}, rows);
        int pick = Console.readInt("Pick a proposal (0 = back): ", 0, proposals.size());
        if (pick == 0) {
            return;
        }
        Subject s = proposals.get(pick - 1);
        int action = Console.menu("Decision for " + s.getName(), "Approve", "Reject");
        try {
            if (action == 1) {
                app.headService().approveSubjectProposal(s.getId(), head.getId());
                Console.println("Approved.");
            } else if (action == 2) {
                app.headService().rejectSubjectProposal(s.getId(), head.getId());
                Console.println("Rejected.");
            }
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    // ---- terms, exams & grading ----------------------------------------------------

    private void termsExamsGradingMenu() {
        boolean in = true;
        while (in) {
            switch (Console.menu("Terms, exams & grading",
                    "Terms", "Exam types", "Grade descriptors")) {
                case 1 -> terms();
                case 2 -> examTypes();
                case 3 -> gradeDescriptors();
                default -> in = false;
            }
        }
    }

    private void terms() {
        List<Term> terms = app.headService().terms(schoolCode);
        terms.sort(Comparator.comparing(Term::getStartDate).reversed());
        List<String[]> rows = new ArrayList<>();
        for (Term t : terms) {
            rows.add(new String[]{t.getName(), nullToEmpty(t.getAcademicYear()),
                    t.getStartDate().toString(), t.getEndDate().toString(), t.isActive() ? "ACTIVE" : ""});
        }
        Console.table(new String[]{"Term", "Year", "Starts", "Ends", ""}, rows);
        if (Console.menu("Terms", "Add a term (becomes the active one)") == 1) {
            String name = Console.readLine("Term name (e.g. Term 1): ");
            String year = Console.readLineOrEmpty("Academic year (e.g. 2026): ");
            LocalDate start = Console.readDate("Start date");
            LocalDate end = Console.readDate("End date");
            try {
                Term t = app.headService().addTerm(schoolCode, name, year, start, end, head.getId());
                Console.println("Term \"" + t.getName() + "\" added and set active.");
            } catch (ServiceException ex) {
                Console.println("Failed: " + ex.getMessage());
            }
        }
        Console.pause();
    }

    private void examTypes() {
        List<ExamType> exams = app.headService().examTypes(schoolCode);
        exams.sort(Comparator.comparing(ExamType::getName));
        List<String[]> rows = new ArrayList<>();
        for (ExamType e : exams) {
            rows.add(new String[]{e.getName(), String.valueOf(e.getWeight())});
        }
        Console.table(new String[]{"Exam type", "Weight (%)"}, rows);
        if (Console.menu("Exam types", "Add an exam type") == 1) {
            String name = Console.readLine("Name (e.g. End Term): ");
            int weight = Console.readInt("Weight (0-100): ", 0, 100);
            try {
                ExamType e = app.headService().addExamType(schoolCode, name, weight, head.getId());
                Console.println("Exam type \"" + e.getName() + "\" added.");
            } catch (ServiceException ex) {
                Console.println("Failed: " + ex.getMessage());
            }
        }
        Console.pause();
    }

    private void gradeDescriptors() {
        List<GradeDescriptor> descriptors = app.headService().gradeDescriptors(schoolCode);
        descriptors.sort(Comparator.comparingInt(GradeDescriptor::getMinScore).reversed());
        List<String[]> rows = new ArrayList<>();
        for (GradeDescriptor g : descriptors) {
            rows.add(new String[]{g.getName(), g.getMinScore() + "-" + g.getMaxScore()});
        }
        Console.table(new String[]{"Descriptor", "Band"}, rows);
        if (Console.menu("Grade descriptors", "Add a descriptor") == 1) {
            String name = Console.readLine("Label (e.g. Exceeding Expectation): ");
            int min = Console.readInt("Minimum score (0-100): ", 0, 100);
            int max = Console.readInt("Maximum score (0-100): ", 0, 100);
            try {
                GradeDescriptor g = app.headService().addGradeDescriptor(schoolCode, name, min, max, head.getId());
                Console.println("Descriptor \"" + g.getName() + "\" added.");
            } catch (ServiceException ex) {
                Console.println("Failed: " + ex.getMessage());
            }
        }
        Console.pause();
    }

    // ---- timetable ----------------------------------------------------------------

    private void timetableMenu() {
        boolean in = true;
        while (in) {
            boolean finalised = app.headService().timetableFinalized(schoolCode);
            switch (Console.menu("Timetable" + (finalised ? " (FINALISED)" : ""),
                    "Pending slot proposals (" + app.headService().pendingSlots(schoolCode).size() + ")",
                    "View approved timetable",
                    "Finalise timetable")) {
                case 1 -> pendingSlots();
                case 2 -> viewTimetable();
                case 3 -> finalise();
                default -> in = false;
            }
        }
    }

    private void pendingSlots() {
        List<TimetableSlot> pending = app.headService().pendingSlots(schoolCode);
        pending.sort(Comparator.comparing(TimetableSlot::getDay).thenComparingInt(TimetableSlot::getPeriod));
        if (pending.isEmpty()) {
            Console.println("No pending timetable slots.");
            Console.pause();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < pending.size(); i++) {
            TimetableSlot s = pending.get(i);
            rows.add(new String[]{String.valueOf(i + 1), s.getDay().name(), String.valueOf(s.getPeriod()),
                    className(s.getClassId()), subjectName(s.getSubjectId()), teacherName(s.getTeacherId())});
        }
        Console.table(new String[]{"#", "Day", "Period", "Class", "Subject", "Teacher"}, rows);
        int pick = Console.readInt("Pick a slot (0 = back): ", 0, pending.size());
        if (pick == 0) {
            return;
        }
        TimetableSlot s = pending.get(pick - 1);
        int action = Console.menu("Decision for " + s.getDay() + " period " + s.getPeriod(),
                "Approve", "Reject");
        try {
            if (action == 1) {
                app.headService().approveSlot(s.getId(), head.getId());
                Console.println("Approved.");
            } else if (action == 2) {
                String reason = Console.readLineOrEmpty("Reason (optional): ");
                app.headService().rejectSlot(s.getId(), head.getId(), reason);
                Console.println("Rejected.");
            }
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void viewTimetable() {
        List<TimetableSlot> approved = app.headService().approvedSlots(schoolCode);
        approved.sort(Comparator.comparing(TimetableSlot::getDay).thenComparingInt(TimetableSlot::getPeriod));
        if (approved.isEmpty()) {
            Console.println("No approved slots yet.");
        } else {
            List<String> periods = app.repos().schools.findById(schoolCode)
                    .map(s -> s.getTimetablePeriods()).orElse(List.of());
            TimetableGrid.render(periods, approved,
                    s -> subjectName(s.getSubjectId()) + " / " + className(s.getClassId())
                            + " (" + teacherName(s.getTeacherId()) + ")");
        }
        Console.pause();
    }

    private void finalise() {
        if (app.headService().timetableFinalized(schoolCode)) {
            Console.println("The timetable is already finalised.");
            Console.pause();
            return;
        }
        int confirm = Console.menu("Finalising locks the timetable: teachers can no longer propose slots.",
                "Yes, finalise", "Cancel");
        if (confirm == 1) {
            try {
                app.headService().finaliseTimetable(schoolCode, head.getId());
                Console.println("Timetable finalised.");
            } catch (ServiceException ex) {
                Console.println("Failed: " + ex.getMessage());
            }
        }
        Console.pause();
    }

    // ---- tasks, comments, events ------------------------------------------------

    private void tasksMenu() {
        boolean in = true;
        while (in) {
            switch (Console.menu("Tasks", "Assign a task to a teacher", "All school tasks")) {
                case 1 -> assignTask();
                case 2 -> listTasks();
                default -> in = false;
            }
        }
    }

    private void assignTask() {
        List<User> teachers = app.headService().teachers(schoolCode);
        teachers.sort(Comparator.comparing(User::getName));
        if (teachers.isEmpty()) {
            Console.println("No approved teachers to assign tasks to.");
            Console.pause();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < teachers.size(); i++) {
            rows.add(new String[]{String.valueOf(i + 1), teachers.get(i).getName(), teachers.get(i).getEmail()});
        }
        Console.table(new String[]{"#", "Teacher", "Email"}, rows);
        int pick = Console.readInt("Assign to which teacher? (0 = back): ", 0, teachers.size());
        if (pick == 0) {
            return;
        }
        User teacher = teachers.get(pick - 1);
        String title = Console.readLine("Task title: ");
        String description = Console.readLineOrEmpty("Description (optional): ");
        LocalDate due = readOptionalDate("Due date");
        try {
            app.headService().assignTask(schoolCode, title, description, due, teacher.getId(), head.getId());
            Console.println("Task assigned to " + teacher.getName() + ".");
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void listTasks() {
        List<DutyTask> tasks = app.headService().tasks(schoolCode);
        tasks.sort(Comparator.comparing((DutyTask t) -> t.isCompleted())
                .thenComparing(DutyTask::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())));
        List<String[]> rows = new ArrayList<>();
        for (DutyTask t : tasks) {
            rows.add(new String[]{t.getTitle(), teacherName(t.getAssignedTo()),
                    t.getDueDate() == null ? "-" : t.getDueDate().toString(),
                    t.isCompleted() ? "Done" : "Open"});
        }
        Console.table(new String[]{"Task", "Assigned to", "Due", "Status"}, rows);
        Console.pause();
    }

    private void overallComments() {
        List<Student> students = app.headService().students(schoolCode);
        List<Student> active = students.stream()
                .filter(s -> s.getEnrollmentStatus() == EnrollmentStatus.ACTIVE).toList();
        if (active.isEmpty()) {
            Console.println("No active students.");
            Console.pause();
            return;
        }
        active.sort(Comparator.comparing(Student::getName));
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < active.size(); i++) {
            rows.add(new String[]{String.valueOf(i + 1), active.get(i).getName(),
                    active.get(i).getAdmissionNumber(), className(active.get(i).getClassId())});
        }
        Console.table(new String[]{"#", "Student", "Adm No", "Class"}, rows);
        int pick = Console.readInt("Pick a student (0 = back): ", 0, active.size());
        if (pick == 0) {
            return;
        }
        Student s = active.get(pick - 1);
        String current = app.repos().comments.findFirst(c -> s.getId().equals(c.getStudentId()))
                .map(OverallComment::getComment).orElse("");
        if (!current.isBlank()) {
            Console.println("Current comment: " + current);
        }
        String comment = Console.readLine("New overall comment: ");
        try {
            app.headService().setOverallComment(s.getId(), comment, head.getId());
            Console.println("Comment saved for " + s.getName() + ".");
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void eventsMenu() {
        List<CalendarEvent> events = app.headService().events(schoolCode);
        events.sort(Comparator.comparing(CalendarEvent::getStartDate));
        List<String[]> rows = new ArrayList<>();
        for (CalendarEvent e : events) {
            rows.add(new String[]{e.getTitle(), e.getStartDate().toString(),
                    e.getEndDate() == null ? "" : e.getEndDate().toString(), e.isHoliday() ? "Holiday" : ""});
        }
        Console.table(new String[]{"Event", "From", "To", ""}, rows);
        if (Console.menu("Calendar", "Add an event") == 1) {
            String title = Console.readLine("Event title: ");
            LocalDate start = Console.readDate("Start date");
            LocalDate end = readOptionalDate("End date");
            int holiday = Console.menu("Is this a holiday?", "Yes", "No");
            try {
                app.headService().addEvent(schoolCode, title, start, end, holiday == 1, head.getId());
                Console.println("Event added.");
            } catch (ServiceException ex) {
                Console.println("Failed: " + ex.getMessage());
            }
        }
        Console.pause();
    }

    // ---- analytics & dashboard -----------------------------------------------------

    private void analyticsMenu() {
        Term term = app.analytics().currentTerm(schoolCode);
        if (term == null) {
            Console.println("No terms yet — add one under \"Terms, exams & grading\" first.");
            Console.pause();
            return;
        }
        switch (Console.menu("Analytics (" + term.getName() + " " + nullToEmpty(term.getAcademicYear()) + ")",
                "Students x subjects matrix", "Class vs school performance")) {
            case 1 -> matrix(term);
            case 2 -> classPerformance(term);
            default -> {
            }
        }
    }

    private void matrix(Term term) {
        AnalyticsMatrix m = app.analytics().matrix(schoolCode, term.getId());
        if (m.getRows().isEmpty()) {
            Console.println("No active students or marks for this term yet.");
            Console.pause();
            return;
        }
        List<String> subjects = m.getSubjectNames();
        String[] headers = new String[subjects.size() + 2];
        headers[0] = "Student";
        for (int i = 0; i < subjects.size(); i++) {
            headers[i + 1] = subjects.get(i);
        }
        headers[headers.length - 1] = "Overall";
        List<String[]> rows = new ArrayList<>();
        for (MatrixRow r : m.getRows()) {
            String[] row = new String[headers.length];
            row[0] = r.getStudentName();
            for (int i = 0; i < subjects.size(); i++) {
                Double avg = r.getAverageFor(subjects.get(i));
                row[i + 1] = avg == null ? "" : String.format("%.1f", avg);
            }
            row[headers.length - 1] = String.format("%.1f", r.getOverallAverage());
            rows.add(row);
        }
        Console.table(headers, rows);
        Console.pause();
    }

    private void classPerformance(Term term) {
        List<ClassPerformance> perf = app.analytics().classPerformance(schoolCode, term.getId());
        if (perf.isEmpty()) {
            Console.println("No classes yet.");
            Console.pause();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (ClassPerformance p : perf) {
            rows.add(new String[]{p.getClassName(), String.format("%.1f", p.getClassAverage()),
                    String.format("%.1f", p.getSchoolAverage()),
                    (p.gap() >= 0 ? "+" : "") + String.format("%.1f", p.gap())});
        }
        Console.table(new String[]{"Class", "Class avg", "School avg", "Gap"}, rows);
        Console.pause();
    }

    private void dashboard() {
        DashboardStats stats = app.headService().stats(schoolCode);
        List<String[]> rows = new ArrayList<>();
        stats.getCounts().forEach((label, count) -> rows.add(new String[]{label, String.valueOf(count)}));
        Console.table(new String[]{"Metric", "Count"}, rows);
        Console.pause();
    }

    private void exportCsv() {
        String dir = Console.readLineOrEmpty("Output directory [exports]: ");
        if (dir.isEmpty()) {
            dir = "exports";
        }
        try {
            Files.createDirectories(Path.of(dir));
            Path file = app.exports().exportStudentsCsv(Path.of(dir), schoolCode);
            Console.println("Written: " + file.toAbsolutePath());
        } catch (IOException | RuntimeException ex) {
            Console.println("Export failed: " + ex.getMessage());
        }
        Console.pause();
    }

    // ---- helpers ---------------------------------------------------------------------

    private String pickClassId() {
        ClassRoom cls = pickClassObject();
        return cls == null ? null : cls.getId();
    }

    private ClassRoom pickClassObject() {
        List<ClassRoom> classes = app.headService().classes(schoolCode);
        classes.sort(Comparator.comparing(ClassRoom::getName));
        if (classes.isEmpty()) {
            Console.println("No classes yet.");
            return null;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < classes.size(); i++) {
            ClassRoom c = classes.get(i);
            rows.add(new String[]{String.valueOf(i + 1), c.getName(), nullToEmpty(c.getStream()),
                    nullToEmpty(c.getAbbreviation())});
        }
        Console.table(new String[]{"#", "Class", "Stream", "Abbr"}, rows);
        int pick = Console.readInt("Pick a class (0 = none): ", 0, classes.size());
        return pick == 0 ? null : classes.get(pick - 1);
    }

    private List<Integer> readMulti(String prompt, int max) {
        while (true) {
            String raw = Console.readLineOrEmpty(prompt);
            if (raw.isEmpty()) {
                return List.of();
            }
            List<Integer> picks = new ArrayList<>();
            boolean ok = true;
            for (String part : raw.split(",")) {
                try {
                    int v = Integer.parseInt(part.trim());
                    if (v < 1 || v > max) {
                        ok = false;
                    }
                    picks.add(v);
                } catch (NumberFormatException e) {
                    ok = false;
                }
            }
            if (ok && !picks.isEmpty()) {
                return picks;
            }
            Console.println("  (comma-separated numbers between 1 and " + max + ")");
        }
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

    private String teacherName(String teacherId) {
        return teacherId == null ? "-"
                : app.repos().users.findById(teacherId).map(User::getName).orElse(teacherId);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
