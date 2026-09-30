package com.cdb.cms.dto;

import com.cdb.cms.model.Student;
import com.cdb.cms.model.Term;

import java.util.List;

/**
 * Unified per-student metrics, assembled once and consumed by the parent view,
 * the teacher console and the report-card export. Mirrors the JavaScript
 * sharedMetrics.js: attendance rate, mean average, remarks, plus the class
 * teacher's and head's names for display on the report card.
 */
public class StudentMetrics {

    /** One subject/exam entry on the report card. */
    public record SubjectScore(String subjectName, String examTypeName,
                               double rawScore, String descriptor) {
    }

    private final Student student;
    private final String className;
    private final Term term;
    private final double attendanceRate;   // 0-100
    private final double average;          // mean of raw scores in the term
    private final String remarks;
    private final String classTeacherName;
    private final String headName;
    private final List<SubjectScore> scores;

    public StudentMetrics(Student student, String className, Term term,
                          double attendanceRate, double average, String remarks,
                          String classTeacherName, String headName,
                          List<SubjectScore> scores) {
        this.student = student;
        this.className = className;
        this.term = term;
        this.attendanceRate = attendanceRate;
        this.average = average;
        this.remarks = remarks;
        this.classTeacherName = classTeacherName;
        this.headName = headName;
        this.scores = List.copyOf(scores);
    }

    public Student getStudent() {
        return student;
    }

    public String getClassName() {
        return className;
    }

    public Term getTerm() {
        return term;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public double getAverage() {
        return average;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getClassTeacherName() {
        return classTeacherName;
    }

    public String getHeadName() {
        return headName;
    }

    public List<SubjectScore> getScores() {
        return scores;
    }
}
