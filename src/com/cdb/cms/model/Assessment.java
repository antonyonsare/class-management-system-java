package com.cdb.cms.model;

import com.cdb.cms.repository.Identifiable;

/**
 * A single mark entered by a teacher for one student in one subject, exam
 * series and term. Maps to the Firestore 'assessments' collection.
 * Uniqueness rule (from the reference app): one assessment per
 * (student, learningAreaId, examTypeId, termId); re-entering a mark updates it.
 */
public class Assessment implements Identifiable {
    private String id;
    private String studentId;
    private String schoolId;
    private String learningAreaId; // subject id
    private String examTypeId;
    private String termId;
    private double rawScore;       // 0-100
    private String descriptor;     // resolved from the school's grade descriptors
    private String teacherId;

    public Assessment() {
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

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public String getLearningAreaId() {
        return learningAreaId;
    }

    public void setLearningAreaId(String learningAreaId) {
        this.learningAreaId = learningAreaId;
    }

    public String getExamTypeId() {
        return examTypeId;
    }

    public void setExamTypeId(String examTypeId) {
        this.examTypeId = examTypeId;
    }

    public String getTermId() {
        return termId;
    }

    public void setTermId(String termId) {
        this.termId = termId;
    }

    public double getRawScore() {
        return rawScore;
    }

    public void setRawScore(double rawScore) {
        this.rawScore = rawScore;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }
}
