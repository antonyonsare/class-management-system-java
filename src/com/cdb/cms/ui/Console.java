package com.cdb.cms.ui;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Console input/presentation helpers shared by every role UI: prompts with
 * validation loops, numbered menus, and simple fixed-width tables. All reads
 * tolerate blank input by re-prompting, so no scanner surprises propagate up.
 */
public final class Console {

    private static final Scanner IN =
            new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    private Console() {
    }

    public static void title(String text) {
        System.out.println();
        System.out.println("== " + text + " ==");
    }

    public static void println() {
        System.out.println();
    }

    public static void println(String text) {
        System.out.println(text);
    }

    public static void printf(String format, Object... args) {
        System.out.printf(format, args);
    }

    /** Raised when stdin reaches EOF (closed pipe / Ctrl-Z Enter); treated as a polite shutdown request. */
    public static final class EndOfInput extends RuntimeException {
    }

    public static String readLine(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!IN.hasNextLine()) {
                throw new EndOfInput();
            }
            String line = IN.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("  (input required)");
        }
    }

    public static String readLineOrEmpty(String prompt) {
        System.out.print(prompt);
        return IN.hasNextLine() ? IN.nextLine().trim() : "";
    }

    /** Reads a password without echo when a real console is attached, else falls back to a plain line. */
    public static char[] readPassword(String prompt) {
        java.io.Console console = System.console();
        if (console != null) {
            char[] pw = console.readPassword("%s", prompt);
            return pw == null ? new char[0] : pw;
        }
        return readLine(prompt).toCharArray();
    }

    public static int readInt(String prompt, int min, int max) {
        while (true) {
            String raw = readLine(prompt);
            try {
                int v = Integer.parseInt(raw);
                if (v >= min && v <= max) {
                    return v;
                }
            } catch (NumberFormatException ignored) {
                // fall through to re-prompt
            }
            System.out.println("  (enter a whole number between " + min + " and " + max + ")");
        }
    }

    public static double readDouble(String prompt, double min, double max) {
        while (true) {
            String raw = readLine(prompt);
            try {
                double v = Double.parseDouble(raw);
                if (v >= min && v <= max) {
                    return v;
                }
            } catch (NumberFormatException ignored) {
                // fall through to re-prompt
            }
            System.out.println("  (enter a number between " + trim(min) + " and " + trim(max) + ")");
        }
    }

    public static LocalDate readDate(String prompt) {
        while (true) {
            String raw = readLine(prompt + " (yyyy-mm-dd)");
            try {
                return LocalDate.parse(raw);
            } catch (DateTimeParseException e) {
                System.out.println("  (use the format 2026-01-31)");
            }
        }
    }

    public static LocalDate readDateOrToday(String prompt) {
        String raw = readLineOrEmpty(prompt + " (yyyy-mm-dd, Enter = today)");
        if (raw.isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            System.out.println("  (unparseable date, using today)");
            return LocalDate.now();
        }
    }

    /**
     * Prints a numbered menu (options 1..n, then "0. Back") and reads a choice
     * in 0..n. Callers switch on the returned value; 0 universally means
     * "go back / log out".
     */
    public static int menu(String heading, String... options) {
        println();
        System.out.println("-- " + heading + " --");
        for (int i = 0; i < options.length; i++) {
            System.out.println("  " + (i + 1) + ". " + options[i]);
        }
        System.out.println("  0. Back / Log out");
        return readInt("Choose: ", 0, options.length);
    }

    public static void pause() {
        System.out.print("Press Enter to continue...");
        if (IN.hasNextLine()) {
            IN.nextLine();
        }
    }

    /** Renders a plain fixed-width table; long cells are truncated. */
    public static void table(String[] headers, List<String[]> rows) {
        int cols = headers.length;
        int[] widths = new int[cols];
        for (int c = 0; c < cols; c++) {
            widths[c] = headers[c].length();
        }
        for (String[] row : rows) {
            for (int c = 0; c < cols && c < row.length; c++) {
                widths[c] = Math.max(widths[c], row[c] == null ? 0 : row[c].length());
                widths[c] = Math.min(widths[c], 38);
            }
        }
        StringBuilder line = new StringBuilder();
        appendRow(line, headers, widths);
        line.append('\n');
        StringBuilder sep = new StringBuilder();
        for (int c = 0; c < cols; c++) {
            sep.append("-".repeat(widths[c]));
            if (c + 1 < cols) {
                sep.append("---");
            }
        }
        System.out.println(trimEnd(line.toString()));
        System.out.println(sep);
        for (String[] row : rows) {
            StringBuilder rb = new StringBuilder();
            appendRow(rb, row, widths);
            System.out.println(trimEnd(rb.toString()));
        }
        if (rows.isEmpty()) {
            System.out.println("(none)");
        }
    }

    private static void appendRow(StringBuilder sb, String[] cells, int[] widths) {
        for (int c = 0; c < widths.length; c++) {
            String cell = c < cells.length && cells[c] != null ? cells[c] : "";
            if (cell.length() > widths[c]) {
                cell = cell.substring(0, widths[c] - 1) + "~";
            }
            sb.append(String.format("%-" + widths[c] + "s", cell));
            if (c + 1 < widths.length) {
                sb.append(" | ");
            }
        }
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format("%.2f", d);
    }

    /** Java 11+ String.trimEnd equivalent (project targets modern JDKs; kept for compatibility). */
    private static String trimEnd(String s) {
        return s.replaceAll("\\s+$", "");
    }
}
