package com.cdb.cms.service;

import com.cdb.cms.dto.StudentMetrics;
import com.cdb.cms.model.Assessment;
import com.cdb.cms.model.CalendarEvent;
import com.cdb.cms.model.ClassRoom;
import com.cdb.cms.model.ExamType;
import com.cdb.cms.model.School;
import com.cdb.cms.model.Student;
import com.cdb.cms.model.Subject;
import com.cdb.cms.model.Term;
import com.cdb.cms.repository.Repositories;
import com.cdb.cms.util.CsvWriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Library-free exports, replacing SheetJS (xlsx), html2pdf and the in-browser
 * ICS builder of the web reference:
 *
 * <ul>
 *   <li>CSV  — flat tabular exports; opens directly in Excel/LibreOffice</li>
 *   <li>ICS  — iCalendar feed of school events for Outlook/Google Calendar</li>
 *   <li>HTML — printable report card; "print to PDF" from any browser</li>
 * </ul>
 *
 * Everything is written into a caller-chosen directory; the console layer
 * prints the resulting path.
 */
public class ExportService {

    private static final DateTimeFormatter ICS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final Repositories repos;

    public ExportService(Repositories repos) {
        this.repos = repos;
    }

    // ---- CSV ------------------------------------------------------------------

    /** Superadmin's enriched schools table. */
    public Path exportSchoolsCsv(Path dir, SuperAdminService superAdmin) {
        List<String[]> rows = new ArrayList<>();
        for (School s : superAdmin.schools()) {
            rows.add(new String[]{
                    s.getSchoolCode(), s.getName(), superAdmin.headNameOf(s),
                    String.valueOf(superAdmin.teacherCount(s.getSchoolCode())),
                    String.valueOf(superAdmin.studentCount(s.getSchoolCode())),
                    String.valueOf(superAdmin.classCount(s.getSchoolCode())),
                    String.valueOf(superAdmin.subjectCount(s.getSchoolCode())),
                    s.getCreatedAt() == null ? "" : s.getCreatedAt().toLocalDate().toString()
            });
        }
        Path file = dir.resolve("schools.csv");
        CsvWriter.write(file,
                new String[]{"School Code", "Name", "Head", "Teachers", "Students", "Classes", "Subjects", "Created"},
                rows);
        return file;
    }

    public Path exportStudentsCsv(Path dir, String schoolCode) {
        List<String[]> rows = new ArrayList<>();
        List<Student> students = repos.students.findBy(s -> schoolCode.equals(s.getSchoolId()));
        students.sort(Comparator.comparing(Student::getName));
        for (Student s : students) {
            rows.add(new String[]{
                    s.getAdmissionNumber(), s.getName(),
                    className(s.getClassId()), s.getEnrollmentStatus().name(),
                    nullToEmpty(s.getParentName()), nullToEmpty(s.getParentPhone()),
                    nullToEmpty(s.getParentToken())
            });
        }
        Path file = dir.resolve("students-" + schoolCode + ".csv");
        CsvWriter.write(file,
                new String[]{"Admission No", "Name", "Class", "Status", "Parent", "Parent Phone", "Parent Token"},
                rows);
        return file;
    }

    public Path exportMarksCsv(Path dir, String classId, String termId) {
        ClassRoom cls = repos.classes.findById(classId).orElse(null);
        Term term = repos.terms.findById(termId).orElse(null);
        List<String> studentIds = repos.students.findBy(s -> classId.equals(s.getClassId())).stream()
                .map(Student::getId).toList();
        List<Assessment> marks = repos.assessments.findBy(a -> termId.equals(a.getTermId())
                && studentIds.contains(a.getStudentId()));
        marks.sort(Comparator.comparing((Assessment a) -> studentName(a.getStudentId()))
                .thenComparing(a -> subjectName(a.getLearningAreaId())));

        List<String[]> rows = new ArrayList<>();
        for (Assessment a : marks) {
            rows.add(new String[]{
                    studentName(a.getStudentId()), subjectName(a.getLearningAreaId()),
                    examTypeName(a.getExamTypeId()),
                    trim(a.getRawScore()), nullToEmpty(a.getDescriptor())
            });
        }
        String base = "marks-" + (cls == null ? classId : cls.getName().replace(' ', '_'))
                + "-" + (term == null ? termId : term.getName().replace(' ', '_')) + ".csv";
        Path file = dir.resolve(base);
        CsvWriter.write(file,
                new String[]{"Student", "Subject", "Exam", "Score", "Descriptor"}, rows);
        return file;
    }

    // ---- ICS -------------------------------------------------------------------

    /** All-day VEVENTs, matching the client-side calendarExport.js output. */
    public String buildIcs(List<CalendarEvent> events, String calendarName) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//CDB Class Management System//EN\r\n");
        sb.append("X-WR-CALNAME:").append(escapeIcs(calendarName)).append("\r\n");
        long stamp = System.currentTimeMillis();
        for (CalendarEvent ev : events) {
            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:").append(ev.getId()).append('@').append("cdb-cms").append("\r\n");
            sb.append("DTSTAMP:").append(ICS_DATE.format(LocalDate.now())).append("T000000Z\r\n");
            sb.append("SUMMARY:").append(escapeIcs(ev.getTitle())).append("\r\n");
            sb.append("DTSTART;VALUE=DATE:").append(ICS_DATE.format(ev.getStartDate())).append("\r\n");
            LocalDate exclusiveEnd = (ev.getEndDate() == null ? ev.getStartDate() : ev.getEndDate()).plusDays(1);
            sb.append("DTEND;VALUE=DATE:").append(ICS_DATE.format(exclusiveEnd)).append("\r\n");
            if (ev.isHoliday()) {
                sb.append("CATEGORIES:Holiday\r\n");
            }
            sb.append("END:VEVENT\r\n");
        }
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    public Path exportIcs(Path dir, String filename, List<CalendarEvent> events, String calendarName) {
        Path file = dir.resolve(filename);
        try {
            Files.writeString(file, buildIcs(events, calendarName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + file, e);
        }
        return file;
    }

    // ---- HTML report card ---------------------------------------------------------

    /** A self-contained, printable report card for one student and term. */
    public String buildReportCardHtml(StudentMetrics m, String schoolName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Report Card - ")
                .append(esc(m.getStudent().getName())).append("</title>");
        sb.append("<style>");
        sb.append("body{font-family:Arial,Helvetica,sans-serif;margin:40px;color:#222;}");
        sb.append("h1{font-size:22px;margin:0;} h2{font-size:15px;margin:4px 0 0;color:#555;}");
        sb.append("table{width:100%;border-collapse:collapse;margin-top:18px;font-size:13px;}");
        sb.append("th,td{border:1px solid #999;padding:6px 8px;text-align:left;}");
        sb.append("th{background:#f0f0f0;} .num{text-align:right;}");
        sb.append(".meta{margin-top:14px;font-size:13px;line-height:1.7;}");
        sb.append(".sig{margin-top:48px;display:flex;justify-content:space-between;font-size:13px;}");
        sb.append("@media print{body{margin:10mm;}}");
        sb.append("</style></head><body>");
        sb.append("<h1>").append(esc(schoolName)).append("</h1>");
        sb.append("<h2>Student Report Card</h2>");
        sb.append("<div class=\"meta\">");
        sb.append("<b>Student:</b> ").append(esc(m.getStudent().getName()))
                .append(" (").append(esc(m.getStudent().getAdmissionNumber())).append(")<br>");
        sb.append("<b>Class:</b> ").append(esc(m.getClassName())).append("<br>");
        if (m.getTerm() != null) {
            sb.append("<b>Term:</b> ").append(esc(m.getTerm().getName()))
                    .append(" &mdash; ").append(esc(m.getTerm().getAcademicYear())).append("<br>");
        }
        sb.append("<b>Attendance rate:</b> ").append(String.format("%.1f", m.getAttendanceRate())).append("%<br>");
        sb.append("<b>Average:</b> ").append(String.format("%.1f", m.getAverage())).append("<br>");
        sb.append("</div>");
        sb.append("<table><thead><tr><th>Subject</th><th>Exam</th><th class=\"num\">Score</th><th>Descriptor</th></tr></thead><tbody>");
        if (m.getScores().isEmpty()) {
            sb.append("<tr><td colspan=\"4\">No marks recorded for this term.</td></tr>");
        }
        for (StudentMetrics.SubjectScore s : m.getScores()) {
            sb.append("<tr><td>").append(esc(s.subjectName())).append("</td><td>")
                    .append(esc(s.examTypeName())).append("</td><td class=\"num\">")
                    .append(trim(s.rawScore())).append("</td><td>")
                    .append(esc(s.descriptor())).append("</td></tr>");
        }
        sb.append("</tbody></table>");
        if (!m.getRemarks().isBlank()) {
            sb.append("<div class=\"meta\"><b>Remarks:</b> ").append(esc(m.getRemarks())).append("</div>");
        }
        sb.append("<div class=\"sig\"><span>Class teacher: ").append(esc(m.getClassTeacherName()))
                .append("</span><span>School head: ").append(esc(m.getHeadName())).append("</span></div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    public Path exportReportCard(Path dir, StudentMetrics m, String schoolName) {
        String safe = m.getStudent().getName().replaceAll("[^A-Za-z0-9]+", "_");
        Path file = dir.resolve("report-card-" + safe + ".html");
        try {
            Files.writeString(file, buildReportCardHtml(m, schoolName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + file, e);
        }
        return file;
    }

    // ---- helpers ----------------------------------------------------------------------

    private String className(String classId) {
        return classId == null ? "-"
                : repos.classes.findById(classId).map(ClassRoom::getName).orElse(classId);
    }

    private String studentName(String studentId) {
        return repos.students.findById(studentId).map(Student::getName).orElse(studentId);
    }

    private String subjectName(String subjectId) {
        return repos.subjects.findById(subjectId).map(Subject::getName).orElse(subjectId);
    }

    private String examTypeName(String examTypeId) {
        return repos.examTypes.findById(examTypeId).map(ExamType::getName).orElse(examTypeId);
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format("%.2f", d);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** ICS TEXT escaping: backslash, semicolon, comma, newlines. */
    private static String escapeIcs(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace(";", "\\;")
                .replace(",", "\\,").replace("\n", "\\n");
    }

    /** HTML text escaping. */
    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
