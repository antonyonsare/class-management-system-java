package com.cdb.cms.model;

import com.cdb.cms.repository.Identifiable;

import java.time.LocalDate;

/**
 * One register entry for one student on one school day.
 * Maps to the Firestore 'attendance' collection.
 * Uniqueness rule (from the reference app): one record per (studentId, date);
 * re-saving the register updates the status.
 */
public class AttendanceRecord implements Identifiable {
    private String id;
    private String studentId;
    private String classId;
    private LocalDate date;
    private AttendanceStatus status;
    private String teacherId;

    public AttendanceRecord() {
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

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }
}
