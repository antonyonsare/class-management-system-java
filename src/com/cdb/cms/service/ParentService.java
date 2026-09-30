package com.cdb.cms.service;

import com.cdb.cms.dto.StudentMetrics;
import com.cdb.cms.model.CalendarEvent;
import com.cdb.cms.model.ClassRoom;
import com.cdb.cms.model.ProposalStatus;
import com.cdb.cms.model.Student;
import com.cdb.cms.model.TimetableSlot;
import com.cdb.cms.repository.Repositories;

import java.util.Comparator;
import java.util.List;

/**
 * Read-only parent view (parent.html?token=... in the reference). Parents hold
 * no account; they present the student's unguessable parent token instead.
 */
public class ParentService {

    private final Repositories repos;
    private final AuthService auth;
    private final AnalyticsService analytics;

    public ParentService(Repositories repos, AuthService auth, AnalyticsService analytics) {
        this.repos = repos;
        this.auth = auth;
        this.analytics = analytics;
    }

    public Student authenticate(String token) {
        return auth.findStudentByParentToken(token)
                .orElseThrow(() -> new ServiceException("Invalid or expired parent token."));
    }

    public StudentMetrics metrics(Student student) {
        return analytics.metrics(student);
    }

    /** Approved weekly slots for the student's class, in weekday/period order. */
    public List<TimetableSlot> timetable(Student student) {
        if (student.getClassId() == null) {
            return List.of();
        }
        List<TimetableSlot> slots = repos.timetableSlots.findBy(s -> student.getClassId().equals(s.getClassId())
                && s.getStatus() == ProposalStatus.APPROVED);
        slots.sort(Comparator.comparing(TimetableSlot::getDay).thenComparingInt(TimetableSlot::getPeriod));
        return slots;
    }

    public String className(Student student) {
        if (student.getClassId() == null) {
            return "-";
        }
        return repos.classes.findById(student.getClassId()).map(ClassRoom::getName).orElse("-");
    }

    /** The school's calendar (holidays and events) for the ICS export. */
    public List<CalendarEvent> events(Student student) {
        List<CalendarEvent> events = repos.calendarEvents.findBy(e -> student.getSchoolId().equals(e.getSchoolId()));
        events.sort(Comparator.comparing(CalendarEvent::getStartDate));
        return events;
    }

    public String schoolName(Student student) {
        return repos.schools.findById(student.getSchoolId()).map(s -> s.getName()).orElse("-");
    }
}
