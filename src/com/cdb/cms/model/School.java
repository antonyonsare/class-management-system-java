package com.cdb.cms.model;

import com.cdb.cms.repository.Identifiable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A school tenant. Maps to the Firestore 'schools' collection; the document id
 * and the human join code are the same value (schoolCode), as in the reference.
 */
public class School implements Identifiable {
    public static final List<String> DEFAULT_PERIODS = List.of(
            "08:00 - 08:40", "08:40 - 09:20", "09:20 - 10:00",
            "10:30 - 11:10", "11:10 - 11:50", "12:30 - 13:10", "13:10 - 13:50");

    private String schoolCode; // acts as the entity id
    private String name;
    private String superHeadId;
    private String motto;
    private String email;
    private String phone;
    private String address;
    private List<String> timetablePeriods;
    private boolean timetableFinalized; // once finalised, teachers can no longer propose slots
    private LocalDateTime createdAt;

    public School() {
        this.timetablePeriods = new ArrayList<>(DEFAULT_PERIODS);
    }

    public School(String schoolCode, String name, String superHeadId, LocalDateTime createdAt) {
        this();
        this.schoolCode = schoolCode;
        this.name = name;
        this.superHeadId = superHeadId;
        this.createdAt = createdAt;
    }

    public String getSchoolCode() {
        return schoolCode;
    }

    public void setSchoolCode(String schoolCode) {
        this.schoolCode = schoolCode;
    }

    // Identifiable bridge: the join code is the entity id.
    @Override
    public String getId() {
        return schoolCode;
    }

    @Override
    public void setId(String id) {
        this.schoolCode = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSuperHeadId() {
        return superHeadId;
    }

    public void setSuperHeadId(String superHeadId) {
        this.superHeadId = superHeadId;
    }

    public String getMotto() {
        return motto;
    }

    public void setMotto(String motto) {
        this.motto = motto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<String> getTimetablePeriods() {
        return timetablePeriods;
    }

    public void setTimetablePeriods(List<String> timetablePeriods) {
        this.timetablePeriods = timetablePeriods;
    }

    public boolean isTimetableFinalized() {
        return timetableFinalized;
    }

    public void setTimetableFinalized(boolean timetableFinalized) {
        this.timetableFinalized = timetableFinalized;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
