package com.cdb.cms.model;

import com.cdb.cms.repository.Identifiable;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A to-do item assigned to a staff member. Maps to the Firestore
 * 'dutyTasks' collection.
 */
public class DutyTask implements Identifiable {
    private String id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private String assignedTo; // user id
    private String schoolId;
    private boolean completed;
    private LocalDateTime createdAt;

    public DutyTask() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
