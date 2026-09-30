package com.cdb.cms.model;

import com.cdb.cms.repository.Identifiable;

import java.time.LocalDate;

/**
 * An academic term. Maps to the Firestore 'terms' collection.
 * At most one term per school is active; activating a term deactivates the
 * rest (same rule as the reference app's "Set active" checkbox).
 */
public class Term implements Identifiable {
    private String id;
    private String name;
    private String academicYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private String schoolId;

    public Term() {
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

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }
}
