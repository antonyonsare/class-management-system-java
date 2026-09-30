package com.cdb.cms.ui;

import com.cdb.cms.dto.DashboardStats;
import com.cdb.cms.model.School;
import com.cdb.cms.model.SchoolHeadApproval;
import com.cdb.cms.model.TeacherApproval;
import com.cdb.cms.model.User;
import com.cdb.cms.service.ServiceException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Superadmin console; mirrors superadmin.js. */
public class SuperAdminConsole {

    private final App app;

    public SuperAdminConsole(App app) {
        this.app = app;
    }

    public void run(User superadmin) {
        boolean in = true;
        while (in) {
            switch (Console.menu("Superadmin console (" + superadmin.getName() + ")",
                    "Pending head registrations",
                    "Pending teacher registrations",
                    "Schools directory",
                    "Send reminder to a school head",
                    "Platform statistics",
                    "Export schools CSV")) {
                case 1 -> heads();
                case 2 -> teachers();
                case 3 -> schools();
                case 4 -> reminder();
                case 5 -> stats();
                case 6 -> exportCsv();
                default -> in = false;
            }
        }
    }

    private void heads() {
        List<SchoolHeadApproval> pending = app.superAdmin().pendingHeads();
        if (pending.isEmpty()) {
            Console.println("No pending head registrations.");
            Console.pause();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < pending.size(); i++) {
            SchoolHeadApproval a = pending.get(i);
            rows.add(new String[]{String.valueOf(i + 1), a.getName(), a.getEmail(),
                    a.getSchoolName() != null ? "New school: " + a.getSchoolName()
                            : "Joins " + a.getSchoolCode(),
                    a.getRequestedAt().toLocalDate().toString()});
        }
        Console.table(new String[]{"#", "Name", "Email", "Request", "Date"}, rows);
        int pick = Console.readInt("Pick a registration (0 = back): ", 0, pending.size());
        if (pick == 0) {
            return;
        }
        SchoolHeadApproval a = pending.get(pick - 1);
        int action = Console.menu("Decision for " + a.getName(), "Approve", "Reject");
        try {
            if (action == 1) {
                app.superAdmin().approveHead(a.getId());
                Console.println("Approved. The head can now log in (a school was created if founding).");
            } else if (action == 2) {
                String reason = Console.readLineOrEmpty("Reason (optional): ");
                app.superAdmin().rejectHead(a.getId(), reason);
                Console.println("Rejected.");
            }
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void teachers() {
        List<TeacherApproval> pending = app.superAdmin().pendingTeachers();
        if (pending.isEmpty()) {
            Console.println("No pending teacher registrations.");
            Console.pause();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < pending.size(); i++) {
            TeacherApproval t = pending.get(i);
            rows.add(new String[]{String.valueOf(i + 1), t.getName(), t.getEmail(),
                    t.getSchoolCode(), t.getRequestedAt().toLocalDate().toString()});
        }
        Console.table(new String[]{"#", "Name", "Email", "School", "Date"}, rows);
        int pick = Console.readInt("Pick a registration (0 = back): ", 0, pending.size());
        if (pick == 0) {
            return;
        }
        TeacherApproval t = pending.get(pick - 1);
        int action = Console.menu("Decision for " + t.getName(), "Approve", "Reject");
        try {
            if (action == 1) {
                app.superAdmin().approveTeacher(t.getId());
                Console.println("Approved.");
            } else if (action == 2) {
                String reason = Console.readLineOrEmpty("Reason (optional): ");
                app.superAdmin().rejectTeacher(t.getId(), reason);
                Console.println("Rejected.");
            }
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void schools() {
        List<String[]> rows = new ArrayList<>();
        for (School s : app.superAdmin().schools()) {
            rows.add(new String[]{s.getSchoolCode(), s.getName(), app.superAdmin().headNameOf(s),
                    String.valueOf(app.superAdmin().teacherCount(s.getSchoolCode())),
                    String.valueOf(app.superAdmin().studentCount(s.getSchoolCode())),
                    String.valueOf(app.superAdmin().classCount(s.getSchoolCode())),
                    String.valueOf(app.superAdmin().subjectCount(s.getSchoolCode()))});
        }
        Console.table(new String[]{"Code", "Name", "Head", "Teachers", "Students", "Classes", "Subjects"}, rows);
        Console.pause();
    }

    private void reminder() {
        List<School> schools = app.superAdmin().schools();
        if (schools.isEmpty()) {
            Console.println("No schools yet.");
            Console.pause();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < schools.size(); i++) {
            School s = schools.get(i);
            rows.add(new String[]{String.valueOf(i + 1), s.getSchoolCode(), s.getName(),
                    app.superAdmin().headNameOf(s)});
        }
        Console.table(new String[]{"#", "Code", "Name", "Head"}, rows);
        int pick = Console.readInt("Pick a school (0 = back): ", 0, schools.size());
        if (pick == 0) {
            return;
        }
        School s = schools.get(pick - 1);
        String message = Console.readLine("Reminder message: ");
        try {
            app.superAdmin().sendReminder(s.getSuperHeadId(), message);
            Console.println("Reminder sent to " + app.superAdmin().headNameOf(s) + ".");
        } catch (ServiceException ex) {
            Console.println("Failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void stats() {
        DashboardStats stats = app.superAdmin().stats();
        List<String[]> rows = new ArrayList<>();
        stats.getCounts().forEach((label, count) -> rows.add(new String[]{label, String.valueOf(count)}));
        Console.table(new String[]{"Metric", "Count"}, rows);
        Console.pause();
    }

    private void exportCsv() {
        String dir = Console.readLineOrEmpty("Output directory [exports]: ");
        if (dir.isEmpty()) {
            dir = "exports";
        }
        try {
            Files.createDirectories(Path.of(dir));
            Path file = app.exports().exportSchoolsCsv(Path.of(dir), app.superAdmin());
            Console.println("Written: " + file.toAbsolutePath());
        } catch (IOException | RuntimeException ex) {
            Console.println("Export failed: " + ex.getMessage());
        }
        Console.pause();
    }
}
