package com.cdb.cms.model;

import com.cdb.cms.repository.Identifiable;

import java.time.LocalDateTime;

/**
 * A pending registration request from a prospective school head.
 * Maps to the Firestore 'schoolHeadApprovals' collection. Either schoolName
 * (request to found a new school) or schoolCode (request to join an existing
 * school) is set, mirroring the radio-button flow in register-head.html.
 */
public class SchoolHeadApproval implements Identifiable {
    private String id;
    private String uid;        // id of the pending user account
    private String name;
    private String email;
    private String schoolName; // non-null when founding a new school
    private String schoolCode; // non-null when joining an existing school
    private LocalDateTime requestedAt;
    private AccountStatus status;

    public SchoolHeadApproval() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
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

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getSchoolCode() {
        return schoolCode;
    }

    public void setSchoolCode(String schoolCode) {
        this.schoolCode = schoolCode;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
