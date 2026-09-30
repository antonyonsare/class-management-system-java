package com.cdb.cms.dto;

import java.util.List;

/**
 * The full students-by-subjects matrix for one school and term: column
 * headers (subject names in a stable order) plus one {@link MatrixRow} per
 * student, sorted by student name as in the reference UI.
 */
public class AnalyticsMatrix {

    private final List<String> subjectNames;
    private final List<MatrixRow> rows;

    public AnalyticsMatrix(List<String> subjectNames, List<MatrixRow> rows) {
        this.subjectNames = List.copyOf(subjectNames);
        this.rows = List.copyOf(rows);
    }

    public List<String> getSubjectNames() {
        return subjectNames;
    }

    public List<MatrixRow> getRows() {
        return rows;
    }
}
