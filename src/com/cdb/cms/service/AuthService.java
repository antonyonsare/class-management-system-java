package com.cdb.cms.service;

import com.cdb.cms.model.AccountStatus;
import com.cdb.cms.model.EnrollmentStatus;
import com.cdb.cms.model.Role;
import com.cdb.cms.model.School;
import com.cdb.cms.model.SchoolHeadApproval;
import com.cdb.cms.model.Student;
import com.cdb.cms.model.TeacherApproval;
import com.cdb.cms.model.User;
import com.cdb.cms.repository.Repositories;
import com.cdb.cms.util.PasswordHasher;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Registration and login. Replaces the Firebase Auth flows of the reference
 * app: credentials are verified locally against salted SHA-256 hashes, and
 * pending/approved/rejected gate-keeping mirrors the JS login redirect logic.
 */
public class AuthService {

    private final Repositories repos;

    public AuthService(Repositories repos) {
        this.repos = repos;
    }

    public boolean needsSuperadminBootstrap() {
        return repos.users.count() == 0;
    }

    /** One-time bootstrap of the first (superadmin) account, like setup-superadmin.js. */
    public User bootstrapSuperadmin(String name, String email, char[] password) {
        if (!needsSuperadminBootstrap()) {
            throw new ServiceException("A superadmin already exists.");
        }
        User u = new User(null, name, email, PasswordHasher.hashPassword(password),
                Role.SUPERADMIN, AccountStatus.APPROVED, null, false, LocalDateTime.now());
        return repos.users.save(u);
    }

    public User login(String email, char[] password) {
        User u = repos.users
                .findFirst(x -> x.getEmail() != null && x.getEmail().equalsIgnoreCase(email.trim()))
                .orElseThrow(() -> new ServiceException("No account found for that email."));
        if (!PasswordHasher.verify(password, u.getPasswordHash())) {
            throw new ServiceException("Incorrect password.");
        }
        if (u.getStatus() == AccountStatus.PENDING) {
            throw new ServiceException("Your account is still awaiting approval by the superadmin.");
        }
        if (u.getStatus() == AccountStatus.REJECTED) {
            throw new ServiceException("Your registration was rejected. Contact the superadmin.");
        }
        return u;
    }

    /**
     * A prospective school head registers either to found a new school
     * (schoolName given) or to join an existing one (schoolCode given).
     */
    public void registerHead(String name, String email, char[] password,
                             String schoolName, String schoolCode) {
        requireNewEmail(email);
        boolean foundingNew = schoolName != null && !schoolName.isBlank();
        if (!foundingNew) {
            if (schoolCode == null || schoolCode.isBlank()) {
                throw new ServiceException("Provide either a new school name or an existing school code.");
            }
            if (repos.schools.findById(schoolCode.trim()).isEmpty()) {
                throw new ServiceException("No school found with code " + schoolCode + ".");
            }
        }
        User u = new User(null, name, email, PasswordHasher.hashPassword(password),
                Role.HEAD, AccountStatus.PENDING, null, foundingNew, LocalDateTime.now());
        repos.users.save(u);

        SchoolHeadApproval a = new SchoolHeadApproval();
        a.setUid(u.getId());
        a.setName(name);
        a.setEmail(email);
        a.setSchoolName(foundingNew ? schoolName.trim() : null);
        a.setSchoolCode(foundingNew ? null : schoolCode.trim());
        a.setRequestedAt(LocalDateTime.now());
        a.setStatus(AccountStatus.PENDING);
        repos.headApprovals.save(a);
    }

    public void registerTeacher(String name, String email, char[] password, String schoolCode) {
        requireNewEmail(email);
        if (schoolCode == null || schoolCode.isBlank()
                || repos.schools.findById(schoolCode.trim()).isEmpty()) {
            throw new ServiceException("Provide a valid school code to register as a teacher.");
        }
        User u = new User(null, name, email, PasswordHasher.hashPassword(password),
                Role.TEACHER, AccountStatus.PENDING, null, false, LocalDateTime.now());
        repos.users.save(u);

        TeacherApproval t = new TeacherApproval();
        t.setUid(u.getId());
        t.setName(name);
        t.setEmail(email);
        t.setSchoolCode(schoolCode.trim());
        t.setRequestedAt(LocalDateTime.now());
        t.setStatus(AccountStatus.PENDING);
        repos.teacherApprovals.save(t);
    }

    /** Parent access: the unguessable token printed on the student's record. */
    public Optional<Student> findStudentByParentToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return repos.students.findFirst(s -> token.trim().equals(s.getParentToken())
                && s.getEnrollmentStatus() == EnrollmentStatus.ACTIVE);
    }

    public void changePassword(User user, char[] oldPassword, char[] newPassword) {
        if (!PasswordHasher.verify(oldPassword, user.getPasswordHash())) {
            throw new ServiceException("Current password is incorrect.");
        }
        if (newPassword.length < 6) {
            throw new ServiceException("New password must be at least 6 characters.");
        }
        user.setPasswordHash(PasswordHasher.hashPassword(newPassword));
        repos.users.save(user);
    }

    private void requireNewEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new ServiceException("A valid email is required.");
        }
        if (repos.users.exists(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email.trim()))) {
            throw new ServiceException("An account with that email already exists.");
        }
    }
}
