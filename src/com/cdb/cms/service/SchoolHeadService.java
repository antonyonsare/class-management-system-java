package com.cdb.cms.service;

import com.cdb.cms.dto.DashboardStats;
import com.cdb.cms.model.AccountStatus;
import com.cdb.cms.model.CalendarEvent;
import com.cdb.cms.model.ClassRoom;
import com.cdb.cms.model.DutyTask;
import com.cdb.cms.model.EnrollmentStatus;
import com.cdb.cms.model.ExamType;
import com.cdb.cms.model.GradeDescriptor;
import com.cdb.cms.model.OverallComment;
import com.cdb.cms.model.ProposalStatus;
import com.cdb.cms.model.Role;
import com.cdb.cms.model.School;
import com.cdb.cms.model.SchoolHeadAudit;
import com.cdb.cms.model.Student;
import com.cdb.cms.model.Subject;
import com.cdb.cms.model.TeacherApproval;
import com.cdb.cms.model.Term;
import com.cdb.cms.model.TimetableSlot;
import com.cdb.cms.model.User;
import com.cdb.cms.repository.Repositories;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * School-head operations, mirroring schoolhead.js: teacher approvals, direct
 * student enrollment (which mints the unguessable parent token), class/subject
 * governance including teacher proposals, terms (single active term per
 * school), exam types, grade descriptors, timetable slot approval and
 * finalisation, task assignment, overall comments, calendar events, and the
 * per-school audit trail.
 */
public class SchoolHeadService {

    private final Repositories repos;
    private final Notifier notifier;
    private final SecureRandom random = new SecureRandom();

    public SchoolHeadService(Repositories repos, Notifier notifier) {
        this.repos = repos;
        this.notifier = notifier;
    }

    // ---- teacher approvals -------------------------------------------------

    public List<TeacherApproval> pendingTeachers(String schoolCode) {
        return repos.teacherApprovals.findBy(t -> t.getStatus() == AccountStatus.PENDING
                && schoolCode.equals(t.getSchoolCode()));
    }

    public void approveTeacher(String approvalId, String headId) {
        TeacherApproval t = pendingTeacher(approvalId);
        t.setStatus(AccountStatus.APPROVED);
        repos.teacherApprovals.save(t);
        repos.users.findById(t.getUid()).ifPresent(u -> {
            u.setStatus(AccountStatus.APPROVED);
            u.setSchoolId(t.getSchoolCode());
            repos.users.save(u);
            notifier.notify(u.getId(), "APPROVAL", "Your teacher account was approved by the school head.");
        });
        audit(headId, "Approved teacher " + t.getName());
    }

    public void rejectTeacher(String approvalId, String headId, String reason) {
        TeacherApproval t = pendingTeacher(approvalId);
        t.setStatus(AccountStatus.REJECTED);
        repos.teacherApprovals.save(t);
        repos.users.findById(t.getUid()).ifPresent(u -> {
            u.setStatus(AccountStatus.REJECTED);
            repos.users.save(u);
        });
        audit(headId, "Rejected teacher " + t.getName());
    }

    public List<User> teachers(String schoolCode) {
        return repos.users.findBy(u -> u.getRole() == Role.TEACHER
                && u.getStatus() == AccountStatus.APPROVED
                && schoolCode.equals(u.getSchoolId()));
    }

    // ---- students ----------------------------------------------------------

    /**
     * Direct enrollment by the head: the student is ACTIVE immediately and an
     * unguessable parentToken is generated, as in schoolhead.js
     * (two base36 random segments concatenated).
     */
    public Student enrollStudent(String schoolCode, String name, String admissionNumber,
                                 String classId, String parentName, String parentPhone,
                                 String headId) {
        requireText(name, "Student name");
        if (classId != null && repos.classes.findById(classId).isEmpty()) {
            throw new ServiceException("Class not found.");
        }
        Student s = new Student();
        s.setName(name.trim());
        s.setAdmissionNumber((admissionNumber == null || admissionNumber.isBlank())
                ? nextAdmissionNumber(schoolCode)
                : admissionNumber.trim());
        s.setClassId(classId);
        s.setSchoolId(schoolCode);
        s.setParentName(parentName);
        s.setParentPhone(parentPhone);
        s.setEnrollmentStatus(EnrollmentStatus.ACTIVE);
        s.setParentToken(generateParentToken());
        repos.students.save(s);
        audit(headId, "Enrolled student " + s.getName() + " (" + s.getAdmissionNumber() + ")");
        return s;
    }

    public List<Student> students(String schoolCode) {
        return repos.students.findBy(s -> schoolCode.equals(s.getSchoolId()));
    }

    public void approveStudent(String studentId, String headId) {
        Student s = repos.students.findById(studentId)
                .orElseThrow(() -> new ServiceException("Student not found."));
        s.setEnrollmentStatus(EnrollmentStatus.ACTIVE);
        repos.students.save(s);
        audit(headId, "Approved student " + s.getName());
    }

    public void rejectStudent(String studentId, String headId) {
        Student s = repos.students.findById(studentId)
                .orElseThrow(() -> new ServiceException("Student not found."));
        s.setEnrollmentStatus(EnrollmentStatus.REJECTED);
        repos.students.save(s);
        audit(headId, "Rejected student " + s.getName());
    }

    // ---- classes -----------------------------------------------------------

    public ClassRoom addClass(String schoolCode, String name, String stream,
                              String abbreviation, String headId) {
        requireText(name, "Class name");
        ClassRoom c = new ClassRoom();
        c.setName(name.trim());
        c.setStream(stream);
        c.setAbbreviation(abbreviation);
        c.setSchoolId(schoolCode);
        c.setStatus(ProposalStatus.APPROVED);
        repos.classes.save(c);
        audit(headId, "Added class " + c.getName());
        return c;
    }

    public List<ClassRoom> classes(String schoolCode) {
        return repos.classes.findBy(c -> schoolCode.equals(c.getSchoolId())
                && c.getStatus() == ProposalStatus.APPROVED);
    }

    public List<ClassRoom> classProposals(String schoolCode) {
        return repos.classes.findBy(c -> schoolCode.equals(c.getSchoolId())
                && c.getStatus() == ProposalStatus.PENDING);
    }

    public void approveClassProposal(String classId, String headId) {
        ClassRoom c = proposal(classId);
        c.setStatus(ProposalStatus.APPROVED);
        repos.classes.save(c);
        notifyTeacherOf(c.getSchoolId(), "Your proposed class \"" + c.getName() + "\" was approved.");
        audit(headId, "Approved class proposal " + c.getName());
    }

    public void rejectClassProposal(String classId, String headId) {
        ClassRoom c = proposal(classId);
        c.setStatus(ProposalStatus.REJECTED);
        repos.classes.save(c);
        notifyTeacherOf(c.getSchoolId(), "Your proposed class \"" + c.getName() + "\" was rejected.");
        audit(headId, "Rejected class proposal " + c.getName());
    }

    public void setClassTeachers(String classId, List<String> teacherIds, String headId) {
        ClassRoom c = mustFindClass(classId);
        c.setTeacherIds(List.copyOf(teacherIds));
        repos.classes.save(c);
        audit(headId, "Set teachers of class " + c.getName());
    }

    public void setClassSubjects(String classId, List<String> subjectIds, String headId) {
        ClassRoom c = mustFindClass(classId);
        c.setSubjectIds(List.copyOf(subjectIds));
        repos.classes.save(c);
        audit(headId, "Set subjects of class " + c.getName());
    }

    // ---- subjects ----------------------------------------------------------

    public Subject addSubject(String schoolCode, String name, String abbreviation, String headId) {
        requireText(name, "Subject name");
        Subject s = new Subject();
        s.setName(name.trim());
        s.setAbbreviation(abbreviation);
        s.setSchoolId(schoolCode);
        s.setStatus(ProposalStatus.APPROVED);
        repos.subjects.save(s);
        audit(headId, "Added subject " + s.getName());
        return s;
    }

    public List<Subject> subjects(String schoolCode) {
        return repos.subjects.findBy(s -> schoolCode.equals(s.getSchoolId())
                && s.getStatus() == ProposalStatus.APPROVED);
    }

    public List<Subject> subjectProposals(String schoolCode) {
        return repos.subjects.findBy(s -> schoolCode.equals(s.getSchoolId())
                && s.getStatus() == ProposalStatus.PENDING);
    }

    public void approveSubjectProposal(String subjectId, String headId) {
        Subject s = subjectProposal(subjectId);
        s.setStatus(ProposalStatus.APPROVED);
        repos.subjects.save(s);
        notifyTeacherOf(s.getSchoolId(), "Your proposed subject \"" + s.getName() + "\" was approved.");
        audit(headId, "Approved subject proposal " + s.getName());
    }

    public void rejectSubjectProposal(String subjectId, String headId) {
        Subject s = subjectProposal(subjectId);
        s.setStatus(ProposalStatus.REJECTED);
        repos.subjects.save(s);
        notifyTeacherOf(s.getSchoolId(), "Your proposed subject \"" + s.getName() + "\" was rejected.");
        audit(headId, "Rejected subject proposal " + s.getName());
    }

    // ---- terms, exam types, grade descriptors -------------------------------

    /**
     * Adding a term makes it the active one: every other term of the school is
     * deactivated first (single active term rule from schoolhead.js).
     */
    public Term addTerm(String schoolCode, String name, String academicYear,
                        LocalDate start, LocalDate end, String headId) {
        requireText(name, "Term name");
        for (Term t : repos.terms.findBy(x -> schoolCode.equals(x.getSchoolId()))) {
            if (t.isActive()) {
                t.setActive(false);
                repos.terms.save(t);
            }
        }
        Term t = new Term();
        t.setName(name.trim());
        t.setAcademicYear(academicYear);
        t.setStartDate(start);
        t.setEndDate(end);
        t.setActive(true);
        t.setSchoolId(schoolCode);
        repos.terms.save(t);
        audit(headId, "Added term " + t.getName() + " (" + t.getAcademicYear() + ")");
        return t;
    }

    public List<Term> terms(String schoolCode) {
        return repos.terms.findBy(t -> schoolCode.equals(t.getSchoolId()));
    }

    public ExamType addExamType(String schoolCode, String name, int weight, String headId) {
        requireText(name, "Exam type name");
        ExamType e = new ExamType();
        e.setName(name.trim());
        e.setWeight(weight);
        e.setSchoolId(schoolCode);
        repos.examTypes.save(e);
        audit(headId, "Added exam type " + e.getName());
        return e;
    }

    public List<ExamType> examTypes(String schoolCode) {
        return repos.examTypes.findBy(e -> schoolCode.equals(e.getSchoolId()));
    }

    public GradeDescriptor addGradeDescriptor(String schoolCode, String name,
                                              int min, int max, String headId) {
        requireText(name, "Descriptor name");
        if (min < 0 || max > 100 || min > max) {
            throw new ServiceException("Band must be within 0-100 with min <= max.");
        }
        GradeDescriptor g = new GradeDescriptor();
        g.setName(name.trim());
        g.setMinScore(min);
        g.setMaxScore(max);
        g.setSchoolId(schoolCode);
        repos.gradeDescriptors.save(g);
        audit(headId, "Added grade descriptor " + g.getName() + " (" + min + "-" + max + ")");
        return g;
    }

    public List<GradeDescriptor> gradeDescriptors(String schoolCode) {
        return repos.gradeDescriptors.findBy(g -> schoolCode.equals(g.getSchoolId()));
    }

    // ---- timetable ---------------------------------------------------------

    public List<TimetableSlot> pendingSlots(String schoolCode) {
        return repos.timetableSlots.findBy(s -> schoolCode.equals(s.getSchoolId())
                && s.getStatus() == ProposalStatus.PENDING);
    }

    public List<TimetableSlot> approvedSlots(String schoolCode) {
        return repos.timetableSlots.findBy(s -> schoolCode.equals(s.getSchoolId())
                && s.getStatus() == ProposalStatus.APPROVED);
    }

    public void approveSlot(String slotId, String headId) {
        TimetableSlot s = pendingSlot(slotId);
        s.setStatus(ProposalStatus.APPROVED);
        repos.timetableSlots.save(s);
        notifyTeacher(s.getTeacherId(), "Your timetable slot (" + s.getDay() + " period "
                + s.getPeriod() + ") was approved.");
        audit(headId, "Approved timetable slot " + s.getDay() + " P" + s.getPeriod());
    }

    public void rejectSlot(String slotId, String headId, String reason) {
        TimetableSlot s = pendingSlot(slotId);
        s.setStatus(ProposalStatus.REJECTED);
        repos.timetableSlots.save(s);
        notifyTeacher(s.getTeacherId(), "Your timetable slot (" + s.getDay() + " period "
                + s.getPeriod() + ") was rejected" + ((reason == null || reason.isBlank()) ? "." : ": " + reason.trim()));
        audit(headId, "Rejected timetable slot " + s.getDay() + " P" + s.getPeriod());
    }

    /** Locks the timetable: teachers can no longer propose slots. */
    public void finaliseTimetable(String schoolCode, String headId) {
        School school = repos.schools.findById(schoolCode)
                .orElseThrow(() -> new ServiceException("School not found."));
        school.setTimetableFinalized(true);
        repos.schools.save(school);
        audit(headId, "Finalised the school timetable");
    }

    public boolean timetableFinalized(String schoolCode) {
        return repos.schools.findById(schoolCode).map(School::isTimetableFinalized).orElse(false);
    }

    // ---- tasks, comments, events -------------------------------------------

    public DutyTask assignTask(String schoolCode, String title, String description,
                               LocalDate dueDate, String assignedTo, String headId) {
        requireText(title, "Task title");
        DutyTask t = new DutyTask();
        t.setTitle(title.trim());
        t.setDescription(description);
        t.setDueDate(dueDate);
        t.setAssignedTo(assignedTo);
        t.setSchoolId(schoolCode);
        t.setCompleted(false);
        t.setCreatedAt(LocalDateTime.now());
        repos.tasks.save(t);
        if (assignedTo != null) {
            notifier.notify(assignedTo, "TASK", "New task assigned: " + t.getTitle());
        }
        audit(headId, "Assigned task \"" + t.getTitle() + "\"");
        return t;
    }

    public List<DutyTask> tasks(String schoolCode) {
        return repos.tasks.findBy(t -> schoolCode.equals(t.getSchoolId()));
    }

    public void setOverallComment(String studentId, String comment, String headId) {
        OverallComment c = repos.comments.findFirst(x -> studentId.equals(x.getStudentId()))
                .orElseGet(() -> {
                    OverallComment n = new OverallComment();
                    n.setStudentId(studentId);
                    return n;
                });
        c.setComment(comment);
        c.setUpdatedAt(LocalDateTime.now());
        repos.comments.save(c);
        audit(headId, "Set overall comment for student " + studentId);
    }

    public CalendarEvent addEvent(String schoolCode, String title, LocalDate start,
                                  LocalDate end, boolean holiday, String headId) {
        requireText(title, "Event title");
        CalendarEvent e = new CalendarEvent();
        e.setSchoolId(schoolCode);
        e.setTitle(title.trim());
        e.setStartDate(start);
        e.setEndDate(end == null ? start : end);
        e.setHoliday(holiday);
        repos.calendarEvents.save(e);
        audit(headId, "Added calendar event " + e.getTitle());
        return e;
    }

    public List<CalendarEvent> events(String schoolCode) {
        return repos.calendarEvents.findBy(e -> schoolCode.equals(e.getSchoolId()));
    }

    // ---- dashboard & audit --------------------------------------------------

    public DashboardStats stats(String schoolCode) {
        return new DashboardStats()
                .add("Teachers", teachers(schoolCode).size())
                .add("Students", repos.students.count(s -> schoolCode.equals(s.getSchoolId())))
                .add("Classes", classes(schoolCode).size())
                .add("Subjects", subjects(schoolCode).size())
                .add("Pending teacher approvals", pendingTeachers(schoolCode).size())
                .add("Pending class proposals", classProposals(schoolCode).size())
                .add("Pending subject proposals", subjectProposals(schoolCode).size())
                .add("Pending timetable slots", pendingSlots(schoolCode).size())
                .add("Timetable finalised", timetableFinalized(schoolCode) ? 1 : 0);
    }

    public List<SchoolHeadAudit> auditTrail(String headId) {
        return repos.headAudit.findBy(a -> headId.equals(a.getHeadId()));
    }

    private void audit(String headId, String action) {
        SchoolHeadAudit a = new SchoolHeadAudit();
        a.setHeadId(headId);
        a.setActionDescription(action);
        a.setAt(LocalDateTime.now());
        repos.headAudit.save(a);
    }

    // ---- helpers ------------------------------------------------------------

    private String generateParentToken() {
        return randomBase36() + randomBase36();
    }

    private String randomBase36() {
        return Long.toString(random.nextLong() >>> 16, 36);
    }

    private String nextAdmissionNumber(String schoolCode) {
        long n = repos.students.count(s -> schoolCode.equals(s.getSchoolId())) + 1;
        return String.format("ADM-%04d", n);
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ServiceException(field + " must not be empty.");
        }
    }

    private TeacherApproval pendingTeacher(String approvalId) {
        TeacherApproval t = repos.teacherApprovals.findById(approvalId)
                .orElseThrow(() -> new ServiceException("Teacher registration not found."));
        if (t.getStatus() != AccountStatus.PENDING) {
            throw new ServiceException("That registration has already been decided.");
        }
        return t;
    }

    private ClassRoom mustFindClass(String classId) {
        return repos.classes.findById(classId)
                .orElseThrow(() -> new ServiceException("Class not found."));
    }

    private ClassRoom proposal(String classId) {
        ClassRoom c = mustFindClass(classId);
        if (c.getStatus() != ProposalStatus.PENDING) {
            throw new ServiceException("That proposal has already been decided.");
        }
        return c;
    }

    private Subject subjectProposal(String subjectId) {
        Subject s = repos.subjects.findById(subjectId)
                .orElseThrow(() -> new ServiceException("Subject not found."));
        if (s.getStatus() != ProposalStatus.PENDING) {
            throw new ServiceException("That proposal has already been decided.");
        }
        return s;
    }

    private TimetableSlot pendingSlot(String slotId) {
        TimetableSlot s = repos.timetableSlots.findById(slotId)
                .orElseThrow(() -> new ServiceException("Timetable slot not found."));
        if (s.getStatus() != ProposalStatus.PENDING) {
            throw new ServiceException("That slot has already been decided.");
        }
        return s;
    }

    private void notifyTeacher(String teacherId, String message) {
        notifier.notify(teacherId, "TIMETABLE", message);
    }

    /** Proposals don't record a proposing teacher id; notify all school teachers. */
    private void notifyTeacherOf(String schoolCode, String message) {
        teachers(schoolCode).forEach(t -> notifier.notify(t.getId(), "PROPOSAL", message));
    }
}
