package com.cdb.cms.model;

import com.cdb.cms.repository.Identifiable;

/**
 * An exam series such as "Opener", "Mid-Term", "End-Term".
 * Maps to the Firestore 'examTypes' collection.
 */
public class ExamType implements Identifiable {
    private String id;
    private String name;
    private int weight; // contribution percentage when aggregating series
    private String schoolId;

    public ExamType() {
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

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }
}
