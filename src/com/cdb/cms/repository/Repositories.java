package com.cdb.cms.repository;

import com.cdb.cms.model.Assessment;
import com.cdb.cms.model.AttendanceRecord;
import com.cdb.cms.model.CalendarEvent;
import com.cdb.cms.model.ClassRoom;
import com.cdb.cms.model.DutyTask;
import com.cdb.cms.model.ExamType;
import com.cdb.cms.model.GradeDescriptor;
import com.cdb.cms.model.Notification;
import com.cdb.cms.model.OverallComment;
import com.cdb.cms.model.School;
import com.cdb.cms.model.SchoolHeadApproval;
import com.cdb.cms.model.SchoolHeadAudit;
import com.cdb.cms.model.Student;
import com.cdb.cms.model.Subject;
import com.cdb.cms.model.TeacherApproval;
import com.cdb.cms.model.Term;
import com.cdb.cms.model.TimetableSlot;
import com.cdb.cms.model.User;

/**
 * Dependency-injection container: one repository per entity type, mirroring the
 * Firestore collections of the reference web app. Services receive this
 * container in their constructors and never construct repositories themselves,
 * which is what makes the persistence layer swappable: replacing any
 * {@link InMemoryRepository} here with a Firestore (or local-DB) adapter that
 * honours {@link Repository} changes no other line of code.
 *
 * <p>Id prefixes match the collection's document-id flavour in the reference
 * app so console output and exports stay familiar (usr-1, stu-1, ...).
 */
public class Repositories {

    public final Repository<User> users;
    public final Repository<School> schools;
    public final Repository<SchoolHeadApproval> headApprovals;
    public final Repository<TeacherApproval> teacherApprovals;
    public final Repository<Student> students;
    public final Repository<ClassRoom> classes;
    public final Repository<Subject> subjects;
    public final Repository<Term> terms;
    public final Repository<ExamType> examTypes;
    public final Repository<GradeDescriptor> gradeDescriptors;
    public final Repository<Assessment> assessments;
    public final Repository<AttendanceRecord> attendance;
    public final Repository<TimetableSlot> timetableSlots;
    public final Repository<CalendarEvent> calendarEvents;
    public final Repository<DutyTask> tasks;
    public final Repository<OverallComment> comments;
    public final Repository<Notification> notifications;
    public final Repository<SchoolHeadAudit> headAudit;

    public Repositories() {
        users = new InMemoryRepository<>("usr");
        schools = new InMemoryRepository<>("sch");
        headApprovals = new InMemoryRepository<>("sha");
        teacherApprovals = new InMemoryRepository<>("tap");
        students = new InMemoryRepository<>("stu");
        classes = new InMemoryRepository<>("cls");
        subjects = new InMemoryRepository<>("sub");
        terms = new InMemoryRepository<>("trm");
        examTypes = new InMemoryRepository<>("exm");
        gradeDescriptors = new InMemoryRepository<>("grd");
        assessments = new InMemoryRepository<>("asm");
        attendance = new InMemoryRepository<>("att");
        timetableSlots = new InMemoryRepository<>("tts");
        calendarEvents = new InMemoryRepository<>("evt");
        tasks = new InMemoryRepository<>("tsk");
        comments = new InMemoryRepository<>("cmt");
        notifications = new InMemoryRepository<>("ntf");
        headAudit = new InMemoryRepository<>("aud");
    }
}
