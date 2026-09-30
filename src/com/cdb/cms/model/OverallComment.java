package com.cdb.cms.model;

import com.cdb.cms.repository.Identifiable;

import java.time.LocalDateTime;

/**
 * Free-text conduct/observation remark for a student, shown on report cards.
 * Maps to the Firestore 'overallComments' collection (one per student).
 */
public class OverallComment implements Identifiable {
    private String id;
    private String studentId;
    private String comment;
    private LocalDateTime updatedAt;

    public OverallComment() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
