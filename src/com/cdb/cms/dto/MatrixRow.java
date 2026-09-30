package com.cdb.cms.dto;

import java.util.Map;

/**
 * One row of the students-by-subjects analytics matrix: a student plus their
 * average per subject (subject name -> average, absent when no marks) and the
 * student's overall average. Mirrors a row of analyticsMatrix.js.
 */
public class MatrixRow {

    private final String studentId;
    private final String studentName;
    private final Map<String, Double> subjectAverages; // subject name -> avg
    private final double overallAverage;

    public MatrixRow(String studentId, String studentName,
                     Map<String, Double> subjectAverages, double overallAverage) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.subjectAverages = subjectAverages;
        this.overallAverage = overallAverage;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public Map<String, Double> getSubjectAverages() {
        return subjectAverages;
    }

    public Double getAverageFor(String subjectName) {
        return subjectAverages.get(subjectName);
    }

    public double getOverallAverage() {
        return overallAverage;
    }
}
