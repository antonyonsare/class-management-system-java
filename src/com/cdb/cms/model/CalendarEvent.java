package com.cdb.cms.model;

import com.cdb.cms.repository.Identifiable;

import java.time.LocalDate;

/**
 * A school calendar entry (event, closure or holiday).
 * Maps to the Firestore 'calendarEvents' collection.
 */
public class CalendarEvent implements Identifiable {
    private String id;
    private String schoolId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean holiday;

    public CalendarEvent() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isHoliday() {
        return holiday;
    }

    public void setHoliday(boolean holiday) {
        this.holiday = holiday;
    }
}
