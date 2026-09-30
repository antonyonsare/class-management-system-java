package com.cdb.cms.ui;

import com.cdb.cms.dto.StudentMetrics;
import com.cdb.cms.model.CalendarEvent;
import com.cdb.cms.model.Student;
import com.cdb.cms.model.Subject;
import com.cdb.cms.model.TimetableSlot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parent view (parent.html?token=... in the web reference). Parents hold no
 * account; they present the student's unguessable token and get a read-only
 * report card, timetable, calendar and exports.
 */
public class ParentConsole {

    private final App app;

    public ParentConsole(App app) {
        this.app = app;
    }

    public void run(Student student) {
        boolean in = true;
        while (in) {
            printCard(student);
            switch (Console.menu("Parent view",
                    "Full report on screen",
                    "Export report card (HTML)",
                    "Export school calendar (ICS)",
                    "View weekly timetable",
                    "School events")) {
                case 1 -> fullReport(student);
                case 2 -> exportReportCard(student);
                case 3 -> exportCalendar(student);
                case 4 -> timetable(student);
                case 5 -> events(student);
                default -> in = false;
            }
        }
    }

    private void printCard(Student student) {
        StudentMetrics m = app.parentService().metrics(student);
        Console.println();
        Console.println("  " + m.getStudent().getName() + "  (" + m.getStudent().getAdmissionNumber() + ")");
        Console.println("  " + app.parentService().schoolName(student)
                + "  -  " + (m.getClassName().isBlank() ? "no class" : m.getClassName()));
        if (m.getTerm() != null) {
            Console.println("  " + m.getTerm().getName()
                    + (m.getTerm().getAcademicYear() == null ? "" : " - " + m.getTerm().getAcademicYear()));
        }
        Console.printf("  Attendance: %.1f%%   Average: %.1f%n", m.getAttendanceRate(), m.getAverage());
        if (!m.getRemarks().isBlank()) {
            Console.println("  Remarks: " + m.getRemarks());
        }
    }

    private void fullReport(Student student) {
        StudentMetrics m = app.parentService().metrics(student);
        if (m.getScores().isEmpty()) {
            Console.println("No marks recorded for the current term yet.");
        } else {
            List<String[]> rows = new ArrayList<>();
            for (StudentMetrics.SubjectScore s : m.getScores()) {
                rows.add(new String[]{s.subjectName(), s.examTypeName(),
                        trim(s.rawScore()), s.descriptor()});
            }
            Console.table(new String[]{"Subject", "Exam", "Score", "Descriptor"}, rows);
            Console.printf("%n  Average: %.1f    Attendance: %.1f%%%n", m.getAverage(), m.getAttendanceRate());
            Console.println("  Class teacher: " + m.getClassTeacherName()
                    + "    School head: " + m.getHeadName());
        }
        Console.pause();
    }

    private void exportReportCard(Student student) {
        String dir = Console.readLineOrEmpty("Output directory [exports]: ");
        if (dir.isEmpty()) {
            dir = "exports";
        }
        try {
            Files.createDirectories(Path.of(dir));
            StudentMetrics m = app.parentService().metrics(student);
            Path file = app.exports().exportReportCard(Path.of(dir), m, app.parentService().schoolName(student));
            Console.println("Written: " + file.toAbsolutePath());
            Console.println("Open it in any browser and use \"Print\" to save a PDF.");
        } catch (IOException | RuntimeException ex) {
            Console.println("Export failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void exportCalendar(Student student) {
        List<CalendarEvent> events = app.parentService().events(student);
        if (events.isEmpty()) {
            Console.println("No school events to export.");
            Console.pause();
            return;
        }
        String dir = Console.readLineOrEmpty("Output directory [exports]: ");
        if (dir.isEmpty()) {
            dir = "exports";
        }
        try {
            Files.createDirectories(Path.of(dir));
            String school = app.parentService().schoolName(student);
            Path file = app.exports().exportIcs(Path.of(dir), "school-calendar.ics", events, school);
            Console.println("Written: " + file.toAbsolutePath());
            Console.println("Import it into Outlook, Google Calendar or any calendar app.");
        } catch (IOException | RuntimeException ex) {
            Console.println("Export failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void timetable(Student student) {
        List<TimetableSlot> slots = app.parentService().timetable(student);
        if (slots.isEmpty()) {
            Console.println("No approved timetable slots for your class yet.");
        } else {
            List<String> periods = app.repos().schools.findById(student.getSchoolId())
                    .map(s -> s.getTimetablePeriods()).orElse(List.of());
            TimetableGrid.render(periods, slots,
                    s -> subjectName(s.getSubjectId()));
        }
        Console.pause();
    }

    private void events(Student student) {
        List<CalendarEvent> events = app.parentService().events(student);
        if (events.isEmpty()) {
            Console.println("No school events.");
        } else {
            List<String[]> rows = new ArrayList<>();
            for (CalendarEvent e : events) {
                rows.add(new String[]{e.getTitle(), e.getStartDate().toString(),
                        e.getEndDate() == null || e.getEndDate().equals(e.getStartDate())
                                ? "" : e.getEndDate().toString(),
                        e.isHoliday() ? "Holiday" : ""});
            }
            Console.table(new String[]{"Event", "From", "To", ""}, rows);
        }
        Console.pause();
    }

    private String subjectName(String subjectId) {
        return subjectId == null ? "-"
                : app.repos().subjects.findById(subjectId).map(Subject::getName).orElse(subjectId);
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format("%.2f", d);
    }
}
