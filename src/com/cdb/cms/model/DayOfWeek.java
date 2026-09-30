package com.cdb.cms.model;

/** School week days used by the timetable grid (Monday-Friday). */
public enum DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY;

    public static DayOfWeek fromMenuChoice(int choice) {
        return values()[choice - 1];
    }
}
