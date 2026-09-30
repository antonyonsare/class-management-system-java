package com.cdb.cms.model;

import com.cdb.cms.repository.Identifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * A class cohort. Maps to the Firestore 'classes' collection.
 * Teachers propose classes (PENDING) which the school head must approve,
 * or the head creates them directly (APPROVED).
 */
public class ClassRoom implements Identifiable {
    private String id;
    private String name;
    private String stream;
    private String abbreviation;
    private String schoolId;
    private List<String> teacherIds = new ArrayList<>();
    private List<String> subjectIds = new ArrayList<>();
    private ProposalStatus status;

    public ClassRoom() {
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

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public List<String> getTeacherIds() {
        return teacherIds;
    }

    public void setTeacherIds(List<String> teacherIds) {
        this.teacherIds = teacherIds;
    }

    public List<String> getSubjectIds() {
        return subjectIds;
    }

    public void setSubjectIds(List<String> subjectIds) {
        this.subjectIds = subjectIds;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }
}
