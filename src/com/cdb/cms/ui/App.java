package com.cdb.cms.ui;

import com.cdb.cms.model.Role;
import com.cdb.cms.model.Student;
import com.cdb.cms.model.User;
import com.cdb.cms.repository.Repositories;
import com.cdb.cms.service.AnalyticsService;
import com.cdb.cms.service.AuthService;
import com.cdb.cms.service.ExportService;
import com.cdb.cms.service.Notifier;
import com.cdb.cms.service.ParentService;
import com.cdb.cms.service.SchoolHeadService;
import com.cdb.cms.service.SuperAdminService;
import com.cdb.cms.service.TeacherService;

/**
 * Application shell: wires repositories to services (composition root) and
 * drives the top-level loop — first-run superadmin bootstrap, login,
 * registrations, and routing to the per-role console. Mirrors the role-based
 * page routing of the web reference (auth.js redirecting to superadmin.html /
 * schoolhead.html / teacher.html / parent.html).
 */
public class App {

    private final Repositories repos;
    private final AuthService auth;
    private final Notifier notifier;
    private final SuperAdminService superAdmin;
    private final SchoolHeadService headService;
    private final TeacherService teacherService;
    private final AnalyticsService analytics;
    private final ParentService parentService;
    private final ExportService exports;

    public App(Repositories repos, boolean demo) {
        this.repos = repos;
        this.notifier = new Notifier(repos);
        this.auth = new AuthService(repos);
        this.superAdmin = new SuperAdminService(repos, notifier);
        this.headService = new SchoolHeadService(repos, notifier);
        this.teacherService = new TeacherService(repos);
        this.analytics = new AnalyticsService(repos);
        this.parentService = new ParentService(repos, auth, analytics);
        this.exports = new ExportService(repos);
        if (demo) {
            new DemoDataLoader(this).load();
        }
    }

    public Repositories repos() {
        return repos;
    }

    public AuthService auth() {
        return auth;
    }

    public Notifier notifier() {
        return notifier;
    }

    public SuperAdminService superAdmin() {
        return superAdmin;
    }

    public SchoolHeadService headService() {
        return headService;
    }

    public TeacherService teacherService() {
        return teacherService;
    }

    public AnalyticsService analytics() {
        return analytics;
    }

    public ParentService parentService() {
        return parentService;
    }

    public ExportService exports() {
        return exports;
    }

    public void run() {
        Console.println("==========================================================");
        Console.println(" Circular Digital Bridge - Class Management System (Java)");
        Console.println("==========================================================");
        if (auth.needsSuperadminBootstrap()) {
            bootstrapSuperadmin();
        }
        boolean running = true;
        try {
            while (running) {
                int choice = Console.menu("Welcome",
                        "Log in",
                        "Register as a school head",
                        "Register as a teacher",
                        "Parent access (student token)");
                switch (choice) {
                    case 1 -> login();
                    case 2 -> registerHead();
                    case 3 -> registerTeacher();
                    case 4 -> parentAccess();
                    case 0 -> running = false;
                    default -> {
                    }
                }
            }
        } catch (Console.EndOfInput eof) {
            Console.println("(end of input - shutting down)");
        }
        Console.println("Goodbye.");
    }

    private void bootstrapSuperadmin() {
        Console.title("First run - create the superadmin account");
        Console.println("No accounts exist yet. Create the platform superadmin now.");
        String name = Console.readLine("Full name: ");
        String email = Console.readLine("Email: ");
        char[] password = Console.readPassword("Password (min 6 chars): ");
        try {
            auth.bootstrapSuperadmin(name, email, password);
            Console.println("Superadmin created. You can log in from the menu.");
        } catch (RuntimeException ex) {
            Console.println("Could not create superadmin: " + ex.getMessage());
        }
        Console.pause();
    }

    private void login() {
        String email = Console.readLine("Email: ");
        char[] password = Console.readPassword("Password: ");
        try {
            User user = auth.login(email, password);
            route(user);
        } catch (RuntimeException ex) {
            Console.println("Login failed: " + ex.getMessage());
            Console.pause();
        }
    }

    private void route(User user) {
        if (user.getRole() == Role.SUPERADMIN) {
            new SuperAdminConsole(this).run(user);
        } else if (user.getRole() == Role.HEAD) {
            new SchoolHeadConsole(this).run(user);
        } else if (user.getRole() == Role.TEACHER) {
            new TeacherConsole(this).run(user);
        } else {
            Console.println("No console for role " + user.getRole());
        }
    }

    private void registerHead() {
        Console.title("School head registration");
        String name = Console.readLine("Full name: ");
        String email = Console.readLine("Email: ");
        char[] password = Console.readPassword("Password (min 6 chars): ");
        Console.println("Found a new school, or joining an existing one?");
        int mode = Console.menu("Registration type",
                "Found a new school (you will be its super head)",
                "Join an existing school (I have a school code)");
        String schoolName = null;
        String schoolCode = null;
        if (mode == 1) {
            schoolName = Console.readLine("New school name: ");
        } else if (mode == 2) {
            schoolCode = Console.readLine("Existing school code (e.g. SCH1234): ");
        } else {
            return;
        }
        try {
            auth.registerHead(name, email, password, schoolName, schoolCode);
            Console.println("Registration submitted. You can log in once the superadmin approves it.");
        } catch (RuntimeException ex) {
            Console.println("Registration failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void registerTeacher() {
        Console.title("Teacher registration");
        String name = Console.readLine("Full name: ");
        String email = Console.readLine("Email: ");
        char[] password = Console.readPassword("Password (min 6 chars): ");
        String schoolCode = Console.readLine("School code (e.g. SCH1234): ");
        try {
            auth.registerTeacher(name, email, password, schoolCode);
            Console.println("Registration submitted. You can log in once approved.");
        } catch (RuntimeException ex) {
            Console.println("Registration failed: " + ex.getMessage());
        }
        Console.pause();
    }

    private void parentAccess() {
        Console.title("Parent access");
        String token = Console.readLine("Student token (from the school): ");
        try {
            Student student = parentService.authenticate(token);
            new ParentConsole(this).run(student);
        } catch (RuntimeException ex) {
            Console.println("Access failed: " + ex.getMessage());
            Console.pause();
        }
    }
}
