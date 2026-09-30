package com.cdb.cms;

import com.cdb.cms.repository.Repositories;
import com.cdb.cms.ui.App;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Entry point. Run with {@code --demo} to preload a fully populated demo
 * school (accounts, classes, students, marks, attendance, timetable) so every
 * role can be explored immediately; run without arguments for a clean,
 * first-run-bootstrapped system.
 *
 * <pre>
 *   javac -encoding UTF-8 -d out $(find src -name "*.java")   # Unix
 *   javac -encoding UTF-8 -d out src\com\cdb\cms\Main.java    # Windows (compiles transitively)
 *   java -cp out com.cdb.cms.Main --demo
 * </pre>
 */
public class Main {

    public static void main(String[] args) {
        // Console hosts (notably Windows cmd) default to a legacy charset;
        // force UTF-8 so symbols and em-dashes render correctly when piped.
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        boolean demo = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--demo")) {
                demo = true;
            }
        }
        new App(new Repositories(), demo).run();
    }
}
