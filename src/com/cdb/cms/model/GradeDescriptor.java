package com.cdb.cms.model;

import com.cdb.cms.repository.Identifiable;

/**
 * A score band mapped to a performance label, e.g. 80-100 -> "Exceeding Expectation".
 * Maps to the Firestore 'gradeDescriptors' collection. Entered marks are
 * automatically labelled by the first descriptor whose [min,max] band contains
 * the score.
 */
public class GradeDescriptor implements Identifiable {
    private String id;
    private String name;
    private int minScore;
    private int maxScore;
    private String schoolId;

    public GradeDescriptor() {
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

    public int getMinScore() {
        return minScore;
    }

    public void setMinScore(int minScore) {
        this.minScore = minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public boolean covers(double score) {
        return score >= minScore && score <= maxScore;
    }
}
