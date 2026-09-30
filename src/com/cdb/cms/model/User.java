package com.cdb.cms.model;

import com.cdb.cms.repository.Identifiable;

import java.time.LocalDateTime;

/**
 * A system account. Maps to documents in the Firestore 'users' collection.
 * Passwords are stored as salted SHA-256 hashes (see PasswordHasher) because the
 * offline build has no Firebase Auth; the field is ignored by a future Firestore
 * adapter, which will delegate authentication to Firebase.
 */
public class User implements Identifiable {
    private String id;
    private String name;
    private String email;
    private String passwordHash;
    private Role role;
    private AccountStatus status;
    private String schoolId;   // null for the superadmin
    private boolean superHead; // true for the founding head of a school
    private LocalDateTime createdAt;

    public User() {
    }

    public User(String id, String name, String email, String passwordHash, Role role,
                AccountStatus status, String schoolId, boolean superHead, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.schoolId = schoolId;
        this.superHead = superHead;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public boolean isSuperHead() {
        return superHead;
    }

    public void setSuperHead(boolean superHead) {
        this.superHead = superHead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name + " <" + email + ">";
    }
}
