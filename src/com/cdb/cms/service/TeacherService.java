package com.cdb.cms.service;

import com.cdb.cms.model.Assessment;
import com.cdb.cms.model.AttendanceRecord;
import com.cdb.cms.model.AttendanceStatus;
import com.cdb.cms.model.ClassRoom;
import com.cdb.cms.model.DayOfWeek;
import com.cdb.cms.model.DutyTask;
import com.cdb.cms.model.EnrollmentStatus;
import com.cdb.cms.model.GradeDescriptor;
import com.cdb.cms.model.Notification;
import com.cdb.cms.model.ProposalStatus;
import com.cdb.cms.model.School;
import com.cdb.cms.model.Student;
import com.cdb.cms.model.Subject;
import com.cdb.cms.model.Term;
import com.cdb.cms.model.TimetableSlot;
import com.cdb.cms.repository.Repositories;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Teacher operations, mirroring teacher.js: marks entry with automatic
 * descriptor resolution and upsert per (student, subject, exam type, term),
 * batch attendance per (student, date), timetable slot proposals (blocked once
 * the head finalises), class/subject proposals, and the personal task list.
 */
public class TeacherService {

    private final Repositories repos;

    public TeacherService(Repositories repos) {
        this.repos = repos;
    }

    // ---- classes & students -------------------------------------------------

    public List<ClassRoom> myClasses(String teacherId) {
        return repos.classes.findBy(c -> c.getStatus() == ProposalStatus.APPROVED
                && c.getTeacherIds() != null && c.getTeacherIds().contains(teacherId));
    }

    public List<Student> studentsOf(String classId) {
        return repos.students.findBy(s -> classId.equals(s.getClassId())
                && s.getEnrollmentStatus() == EnrollmentStatus.ACTIVE);
    }

    // ---- marks ---------------------------------------------------------------

    /** First grade descriptor whose [min,max] band contains the score. */
    public String resolveDescriptor(String schoolCode, double score) {
        return repos.gradeDescriptors.findBy(g -> schoolCode.equals(g.getSchoolId())).stream()
                .filter(g -> g.covers(score))
                .map(GradeDescriptor::getName)
                .findFirst()
                .orElse("No descriptor");
    }

    /**
     * Enter (or re-enter) a mark: exactly one assessment exists per
     * (student, subject, exam type, term); re-entry updates the existing row,
     * exactly like the blur-save upsert in teacher.js.
     */
    public Assessment enterMark(String teacherId, String studentId, String schoolCode,
                                String subjectId, String examTypeId, String termId,
                                double rawScore) {
        if (rawScore < 0 || rawScore > 100) {
            throw new ServiceException("Marks must be between 0 and 100.");
        }
        Assessment a = repos.assessments
                .findFirst(x -> studentId.equals(x.getStudentId())
                        && subjectId.equals(x.getLearningAreaId())
                        && examTypeId.equals(x.getExamTypeId())
                        && termId.equals(x.getTermId()))
                .orElseGet(() -> {
                    Assessment n = new Assessment();
                    n.setStudentId(studentId);
                    n.setSchoolId(schoolCode);
                    n.setLearningAreaId(subjectId);
                    n.setExamTypeId(examTypeId);
                    n.setTermId(termId);
                    return n;
                });
        a.setRawScore(rawScore);
        a.setDescriptor(resolveDescriptor(schoolCode, rawScore));
        a.setTeacherId(teacherId);
        return repos.assessments.save(a);
    }

    // ---- attendance ------------------------------------------------------------

    /** Batch save, one row per student for the given date; re-saving updates. */
    public int saveAttendance(String teacherId, String classId, LocalDate date,
                              Map<String, AttendanceStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            throw new ServiceException("No attendance rows to save.");
        }
        int saved = 0;
        for (Map.Entry<String, AttendanceStatus> e : statuses.entrySet()) {
            AttendanceRecord r = repos.attendance
                    .findFirst(x -> e.getKey().equals(x.getStudentId()) && date.equals(x.getDate()))
                    .orElseGet(() -> {
                        AttendanceRecord n = new AttendanceRecord();
                        n.setStudentId(e.getKey());
                        n.setClassId(classId);
                        n.setDate(date);
                        return n;
                    });
            r.setStatus(e.getValue());
            r.setTeacherId(teacherId);
            r.setClassId(classId);
            repos.attendance.save(r);
            saved++;
        }
        return saved;
    }

    // ---- timetable & proposals ---------------------------------------------------

    public TimetableSlot proposeSlot(String teacherId, String schoolCode, String classId,
                                     String subjectId, DayOfWeek day, int period) {
        School school = repos.schools.findById(schoolCode)
                .orElseThrow(() -> new ServiceException("School not found."));
        if (school.isTimetableFinalized()) {
            throw new ServiceException("The timetable has been finalised by the head; no new slots can be proposed.");
        }
        if (period < 1 || period > school.getTimetablePeriods().size()) {
            throw new ServiceException("Period must be between 1 and "
                    + school.getTimetablePeriods().size() + ".");
        }
        TimetableSlot s = new TimetableSlot();
        s.setTeacherId(teacherId);
        s.setSchoolId(schoolCode);
        s.setClassId(classId);
        s.setSubjectId(subjectId);
        s.setDay(day);
        s.setPeriod(period);
        s.setStatus(ProposalStatus.PENDING);
        s.setRequestedAt(LocalDateTime.now());
        return repos.timetableSlots.save(s);
    }

    public List<TimetableSlot> mySlotProposals(String teacherId) {
        return repos.timetableSlots.findBy(s -> teacherId.equals(s.getTeacherId()));
    }

    public ClassRoom proposeClass(String teacherId, String schoolCode,
                                  String name, String stream, String abbreviation) {
        if (name == null || name.isBlank()) {
            throw new ServiceException("Class name must not be empty.");
        }
        ClassRoom c = new ClassRoom();
        c.setName(name.trim());
        c.setStream(stream);
        c.setAbbreviation(abbreviation);
        c.setSchoolId(schoolCode);
        c.setStatus(ProposalStatus.PENDING);
        repos.classes.save(c);
        return c;
    }

    public Subject proposeSubject(String teacherId, String schoolCode,
                                  String name, String abbreviation) {
        if (name == null || name.isBlank()) {
            throw new ServiceException("Subject name must not be empty.");
        }
        Subject s = new Subject();
        s.setName(name.trim());
        s.setAbbreviation(abbreviation);
        s.setSchoolId(schoolCode);
        s.setStatus(ProposalStatus.PENDING);
        repos.subjects.save(s);
        return s;
    }

    // ---- tasks & notifications ----------------------------------------------------

    public List<DutyTask> myTasks(String teacherId) {
        return repos.tasks.findBy(t -> teacherId.equals(t.getAssignedTo()));
    }

    public void completeTask(String taskId) {
        DutyTask t = repos.tasks.findById(taskId)
                .orElseThrow(() -> new ServiceException("Task not found."));
        t.setCompleted(true);
        repos.tasks.save(t);
    }

    public DutyTask addPersonalTask(String schoolCode, String teacherId,
                                    String title, String description, LocalDate dueDate) {
        if (title == null || title.isBlank()) {
            throw new ServiceException("Task title must not be empty.");
        }
        DutyTask t = new DutyTask();
        t.setTitle(title.trim());
        t.setDescription(description);
        t.setDueDate(dueDate);
        t.setAssignedTo(teacherId);
        t.setSchoolId(schoolCode);
        t.setCompleted(false);
        t.setCreatedAt(LocalDateTime.now());
        return repos.tasks.save(t);
    }

    public List<Notification> myNotifications(String teacherId) {
        return repos.notifications.findBy(n -> teacherId.equals(n.getRecipientId()));
    }

    // ---- lookups the console needs -------------------------------------------------

    public List<Subject> subjectsTaught(String teacherId, String schoolCode) {
        List<ClassRoom> mine = myClasses(teacherId);
        if (mine.isEmpty()) {
            return repos.subjects.findBy(s -> schoolCode.equals(s.getSchoolId())
                    && s.getStatus() == ProposalStatus.APPROVED);
        }
        java.util.Set<String> ids = new java.util.LinkedHashSet<>();
        mine.forEach(c -> { if (c.getSubjectIds() != null) ids.addAll(c.getSubjectIds()); });
        List<Subject> fromClasses = repos.subjects.findBy(s -> ids.contains(s.getId())
                && s.getStatus() == ProposalStatus.APPROVED);
        return fromClasses.isEmpty()
                ? repos.subjects.findBy(s -> schoolCode.equals(s.getSchoolId())
                        && s.getStatus() == ProposalStatus.APPROVED)
                : fromClasses;
    }

    public Term activeTerm(String schoolCode) {
        return repos.terms.findFirst(t -> schoolCode.equals(t.getSchoolId()) && t.isActive())
                .orElse(null);
    }
}
