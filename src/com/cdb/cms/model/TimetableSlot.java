package com.cdb.cms.model;

import com.cdb.cms.repository.Identifiable;

import java.time.LocalDateTime;

/**
 * A single cell in the weekly timetable grid proposed by a teacher and
 * approved/rejected by the school head. Maps to the Firestore
 * 'timetableSlots' collection.
 */
public class TimetableSlot implements Identifiable {
    private String id;
    private String teacherId;
    private String schoolId;
    private String classId;
    private String subjectId;
    private DayOfWeek day;
    private int period;            // 1-based index into the school's period list
    private ProposalStatus status;
    private LocalDateTime requestedAt;

    public TimetableSlot() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public DayOfWeek getDay() {
        return day;
    }

    public void setDay(DayOfWeek day) {
        this.day = day;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
}
