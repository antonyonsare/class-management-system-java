package com.cdb.cms.service;

import com.cdb.cms.dto.DashboardStats;
import com.cdb.cms.model.AccountStatus;
import com.cdb.cms.model.Role;
import com.cdb.cms.model.School;
import com.cdb.cms.model.SchoolHeadApproval;
import com.cdb.cms.model.TeacherApproval;
import com.cdb.cms.model.User;
import com.cdb.cms.repository.Repositories;

import java.security.SecureRandom;
import java.util.List;

/**
 * Superadmin operations: gate-keeping head and teacher registrations across
 * all schools, the enriched schools directory, reminders to heads, and
 * platform-wide statistics. Mirrors superadmin.js.
 */
public class SuperAdminService {

    private final Repositories repos;
    private final Notifier notifier;
    private final SecureRandom random = new SecureRandom();

    public SuperAdminService(Repositories repos, Notifier notifier) {
        this.repos = repos;
        this.notifier = notifier;
    }

    public List<SchoolHeadApproval> pendingHeads() {
        return repos.headApprovals.findBy(a -> a.getStatus() == AccountStatus.PENDING);
    }

    public List<TeacherApproval> pendingTeachers() {
        return repos.teacherApprovals.findBy(a -> a.getStatus() == AccountStatus.PENDING);
    }

    /**
     * Approving a head either creates the school (founder flow — a join code
     * "SCH"+4 digits is generated, as in superadmin.js) or links the head to
     * the school named in the request.
     */
    public void approveHead(String approvalId) {
        SchoolHeadApproval a = pendingHead(approvalId);
        User u = repos.users.findById(a.getUid())
                .orElseThrow(() -> new ServiceException("The requesting user account no longer exists."));

        String schoolCode;
        if (a.getSchoolName() != null && !a.getSchoolName().isBlank()) {
            schoolCode = generateSchoolCode();
            School school = new School(schoolCode, a.getSchoolName(), u.getId(),
                    java.time.LocalDateTime.now());
            repos.schools.save(school);
            u.setSuperHead(true);
        } else {
            schoolCode = a.getSchoolCode();
            if (repos.schools.findById(schoolCode).isEmpty()) {
                throw new ServiceException("School " + schoolCode + " no longer exists.");
            }
        }
        u.setSchoolId(schoolCode);
        u.setStatus(AccountStatus.APPROVED);
        repos.users.save(u);
        a.setStatus(AccountStatus.APPROVED);
        repos.headApprovals.save(a);
        notifier.notify(u.getId(), "APPROVAL",
                "Your head account was approved. School code: " + schoolCode);
    }

    public void rejectHead(String approvalId, String reason) {
        SchoolHeadApproval a = pendingHead(approvalId);
        a.setStatus(AccountStatus.REJECTED);
        repos.headApprovals.save(a);
        repos.users.findById(a.getUid()).ifPresent(u -> {
            u.setStatus(AccountStatus.REJECTED);
            repos.users.save(u);
            notifier.notify(u.getId(), "REJECTION",
                    "Your head registration was rejected" + suffix(reason));
        });
    }

    public void approveTeacher(String approvalId) {
        TeacherApproval t = pendingTeacher(approvalId);
        User u = repos.users.findById(t.getUid())
                .orElseThrow(() -> new ServiceException("The requesting user account no longer exists."));
        u.setStatus(AccountStatus.APPROVED);
        u.setSchoolId(t.getSchoolCode());
        repos.users.save(u);
        t.setStatus(AccountStatus.APPROVED);
        repos.teacherApprovals.save(t);
        notifier.notify(u.getId(), "APPROVAL",
                "Your teacher account was approved for school " + t.getSchoolCode() + ".");
    }

    public void rejectTeacher(String approvalId, String reason) {
        TeacherApproval t = pendingTeacher(approvalId);
        t.setStatus(AccountStatus.REJECTED);
        repos.teacherApprovals.save(t);
        repos.users.findById(t.getUid()).ifPresent(u -> {
            u.setStatus(AccountStatus.REJECTED);
            repos.users.save(u);
            notifier.notify(u.getId(), "REJECTION",
                    "Your teacher registration was rejected" + suffix(reason));
        });
    }

    public List<School> schools() {
        return repos.schools.findAll();
    }

    public String headNameOf(School school) {
        return repos.users.findById(school.getSuperHeadId())
                .map(User::getName).orElse("-");
    }

    public long teacherCount(String schoolCode) {
        return repos.users.count(u -> u.getRole() == Role.TEACHER
                && u.getStatus() == AccountStatus.APPROVED
                && schoolCode.equals(u.getSchoolId()));
    }

    public long studentCount(String schoolCode) {
        return repos.students.count(s -> schoolCode.equals(s.getSchoolId()));
    }

    public long classCount(String schoolCode) {
        return repos.classes.count(c -> schoolCode.equals(c.getSchoolId()));
    }

    public long subjectCount(String schoolCode) {
        return repos.subjects.count(s -> schoolCode.equals(s.getSchoolId()));
    }

    public void sendReminder(String headUserId, String message) {
        if (message == null || message.isBlank()) {
            throw new ServiceException("Reminder message must not be empty.");
        }
        notifier.notify(headUserId, "REMINDER", message.trim());
    }

    public DashboardStats stats() {
        return new DashboardStats()
                .add("Pending head registrations", pendingHeads().size())
                .add("Pending teacher registrations", pendingTeachers().size())
                .add("Schools", repos.schools.count())
                .add("Students (all schools)", repos.students.count())
                .add("Approved teachers (all schools)",
                        repos.users.count(u -> u.getRole() == Role.TEACHER
                                && u.getStatus() == AccountStatus.APPROVED));
    }

    private String generateSchoolCode() {
        String code;
        do {
            code = "SCH" + (1000 + random.nextInt(9000));
        } while (repos.schools.findById(code).isPresent());
        return code;
    }

    private SchoolHeadApproval pendingHead(String approvalId) {
        SchoolHeadApproval a = repos.headApprovals.findById(approvalId)
                .orElseThrow(() -> new ServiceException("Head registration not found."));
        if (a.getStatus() != AccountStatus.PENDING) {
            throw new ServiceException("That registration has already been " + a.getStatus().name().toLowerCase() + ".");
        }
        return a;
    }

    private TeacherApproval pendingTeacher(String approvalId) {
        TeacherApproval t = repos.teacherApprovals.findById(approvalId)
                .orElseThrow(() -> new ServiceException("Teacher registration not found."));
        if (t.getStatus() != AccountStatus.PENDING) {
            throw new ServiceException("That registration has already been " + t.getStatus().name().toLowerCase() + ".");
        }
        return t;
    }

    private static String suffix(String reason) {
        return (reason == null || reason.isBlank()) ? "." : ": " + reason.trim();
    }
}
