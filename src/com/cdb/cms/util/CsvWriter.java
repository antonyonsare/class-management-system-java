package com.cdb.cms.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Minimal RFC-4180-style CSV writer (no external library): values containing
 * commas, quotes or newlines are quoted, quotes are doubled. Replaces the
 * SheetJS/XLSX dependency of the web reference — .csv opens in Excel just the
 * same for the flat tabular exports this system produces.
 */
public final class CsvWriter {

    private CsvWriter() {
    }

    public static void write(Path path, String[] header, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, header);
        for (String[] row : rows) {
            appendRow(sb, row);
        }
        try {
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + path, e);
        }
    }

    private static void appendRow(StringBuilder sb, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(values[i]));
        }
        sb.append("\r\n");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
