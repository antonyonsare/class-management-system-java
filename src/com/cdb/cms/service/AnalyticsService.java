package com.cdb.cms.service;

import com.cdb.cms.dto.AnalyticsMatrix;
import com.cdb.cms.dto.ClassPerformance;
import com.cdb.cms.dto.MatrixRow;
import com.cdb.cms.dto.StudentMetrics;
import com.cdb.cms.model.Assessment;
import com.cdb.cms.model.AttendanceStatus;
import com.cdb.cms.model.ClassRoom;
import com.cdb.cms.model.EnrollmentStatus;
import com.cdb.cms.model.ExamType;
import com.cdb.cms.model.GradeDescriptor;
import com.cdb.cms.model.OverallComment;
import com.cdb.cms.model.ProposalStatus;
import com.cdb.cms.model.School;
import com.cdb.cms.model.Student;
import com.cdb.cms.model.Subject;
import com.cdb.cms.model.Term;
import com.cdb.cms.model.User;
import com.cdb.cms.repository.Repositories;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only analytics shared by the parent view, teacher console and school
 * head console: attendance rate, term averages, per-student metrics, the
 * students-by-subjects matrix, and class-vs-school performance. Pure functions
 * over repository state — no writes here.
 */
public class AnalyticsService {

    private final Repositories repos;

    public AnalyticsService(Repositories repos) {
        this.repos = repos;
    }

    /** present / total * 100; 100 when the student has no attendance rows. */
    public double attendanceRate(String studentId) {
        List<com.cdb.cms.model.AttendanceRecord> records =
                repos.attendance.findBy(r -> studentId.equals(r.getStudentId()));
        if (records.isEmpty()) {
            return 100.0;
        }
        long present = records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        return present * 100.0 / records.size();
    }

    /** Mean of the student's raw scores in the term; 0 when no marks exist. */
    public double average(String studentId, String termId) {
        List<Assessment> marks = repos.assessments.findBy(a -> studentId.equals(a.getStudentId())
                && termId.equals(a.getTermId()));
        return marks.stream().mapToDouble(Assessment::getRawScore).average().orElse(0.0);
    }

    public Optional<Term> activeTerm(String schoolCode) {
        return repos.terms.findFirst(t -> schoolCode.equals(t.getSchoolId()) && t.isActive());
    }

    /** The term metrics are reported against: active, else the latest by start date. */
    public Term currentTerm(String schoolCode) {
        Optional<Term> active = activeTerm(schoolCode);
        if (active.isPresent()) {
            return active.get();
        }
        return repos.terms.findBy(t -> schoolCode.equals(t.getSchoolId())).stream()
                .max(Comparator.comparing(Term::getStartDate))
                .orElse(null);
    }

    /**
     * The unified per-student metrics DTO (sharedMetrics.js): identity, class,
     * term, attendance rate, average, remarks (head comment, else the
     * descriptor band covering the average), class teacher and head names,
     * and the per-subject score lines.
     */
    public StudentMetrics metrics(Student student) {
        Term term = currentTerm(student.getSchoolId());

        String className = "-";
        String classTeacherName = "-";
        if (student.getClassId() != null) {
            Optional<ClassRoom> cls = repos.classes.findById(student.getClassId());
            if (cls.isPresent()) {
                ClassRoom c = cls.get();
                className = c.getName() + (c.getStream() == null || c.getStream().isBlank()
                        ? "" : " " + c.getStream());
                if (c.getTeacherIds() != null && !c.getTeacherIds().isEmpty()) {
                    classTeacherName = repos.users.findById(c.getTeacherIds().get(0))
                            .map(User::getName).orElse("-");
                }
            }
        }

        String headName = repos.schools.findById(student.getSchoolId())
                .map(School::getSuperHeadId)
                .flatMap(repos.users::findById)
                .map(User::getName).orElse("-");

        double rate = attendanceRate(student.getId());
        double average = term == null ? 0.0 : average(student.getId(), term.getId());

        String remarks = repos.comments.findFirst(c -> student.getId().equals(c.getStudentId()))
                .map(OverallComment::getComment)
                .filter(s -> !s.isBlank())
                .orElse(descriptorFor(student.getSchoolId(), average));

        List<StudentMetrics.SubjectScore> scores = new ArrayList<>();
        if (term != null) {
            List<Assessment> marks = repos.assessments.findBy(a -> student.getId().equals(a.getStudentId())
                    && term.getId().equals(a.getTermId()));
            marks.sort(Comparator.comparing(a -> subjectName(a.getLearningAreaId())));
            for (Assessment a : marks) {
                scores.add(new StudentMetrics.SubjectScore(
                        subjectName(a.getLearningAreaId()),
                        examTypeName(a.getExamTypeId()),
                        a.getRawScore(),
                        a.getDescriptor()));
            }
        }
        return new StudentMetrics(student, className, term, rate, average, remarks,
                classTeacherName, headName, scores);
    }

    /** Students (rows) x subjects (columns) matrix of term averages. */
    public AnalyticsMatrix matrix(String schoolCode, String termId) {
        List<Subject> subjects = repos.subjects.findBy(s -> schoolCode.equals(s.getSchoolId())
                && s.getStatus() == ProposalStatus.APPROVED);
        subjects.sort(Comparator.comparing(Subject::getName));
        List<String> columns = subjects.stream().map(Subject::getName).toList();

        List<Student> students = repos.students.findBy(s -> schoolCode.equals(s.getSchoolId())
                && s.getEnrollmentStatus() == EnrollmentStatus.ACTIVE);
        students.sort(Comparator.comparing(Student::getName));

        Map<String, String> subjectNamesById = new LinkedHashMap<>();
        subjects.forEach(s -> subjectNamesById.put(s.getId(), s.getName()));

        List<MatrixRow> rows = new ArrayList<>();
        for (Student st : students) {
            Map<String, Double> perSubject = new LinkedHashMap<>();
            List<Assessment> marks = repos.assessments.findBy(a -> st.getId().equals(a.getStudentId())
                    && termId.equals(a.getTermId()));
            Map<String, List<Assessment>> bySubject = new LinkedHashMap<>();
            for (Assessment a : marks) {
                bySubject.computeIfAbsent(a.getLearningAreaId(), k -> new ArrayList<>()).add(a);
            }
            for (Map.Entry<String, List<Assessment>> e : bySubject.entrySet()) {
                String name = subjectNamesById.getOrDefault(e.getKey(), e.getKey());
                double avg = e.getValue().stream().mapToDouble(Assessment::getRawScore).average().orElse(0.0);
                perSubject.put(name, avg);
            }
            double overall = marks.stream().mapToDouble(Assessment::getRawScore).average().orElse(0.0);
            rows.add(new MatrixRow(st.getId(), st.getName(), perSubject, overall));
        }
        return new AnalyticsMatrix(columns, rows);
    }

    /** Each approved class's average against the school average for the term. */
    public List<ClassPerformance> classPerformance(String schoolCode, String termId) {
        double schoolAvg = schoolAverage(schoolCode, termId);
        List<ClassRoom> classes = repos.classes.findBy(c -> schoolCode.equals(c.getSchoolId())
                && c.getStatus() == ProposalStatus.APPROVED);
        classes.sort(Comparator.comparing(ClassRoom::getName));
        List<ClassPerformance> out = new ArrayList<>();
        for (ClassRoom c : classes) {
            List<String> studentIds = repos.students.findBy(s -> c.getId().equals(s.getClassId())
                    && s.getEnrollmentStatus() == EnrollmentStatus.ACTIVE).stream()
                    .map(Student::getId).toList();
            double avg = studentIds.isEmpty() ? 0.0
                    : repos.assessments.findBy(a -> termId.equals(a.getTermId())
                            && studentIds.contains(a.getStudentId())).stream()
                    .mapToDouble(Assessment::getRawScore).average().orElse(0.0);
            out.add(new ClassPerformance(c.getId(), c.getName(), avg, schoolAvg));
        }
        return out;
    }

    public double schoolAverage(String schoolCode, String termId) {
        return repos.assessments.findBy(a -> schoolCode.equals(a.getSchoolId())
                && termId.equals(a.getTermId())).stream()
                .mapToDouble(Assessment::getRawScore).average().orElse(0.0);
    }

    private String descriptorFor(String schoolCode, double score) {
        return repos.gradeDescriptors.findBy(g -> schoolCode.equals(g.getSchoolId())).stream()
                .filter(g -> g.covers(score))
                .map(GradeDescriptor::getName)
                .findFirst().orElse("");
    }

    private String subjectName(String subjectId) {
        return repos.subjects.findById(subjectId).map(Subject::getName).orElse(subjectId);
    }

    private String examTypeName(String examTypeId) {
        return repos.examTypes.findById(examTypeId).map(ExamType::getName).orElse(examTypeId);
    }
}
