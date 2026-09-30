package com.cdb.cms.ui;

import com.cdb.cms.model.DayOfWeek;
import com.cdb.cms.model.TimetableSlot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Shared periods x days renderer for the weekly timetable grid, used by the
 * school head console and the parent view. One cell per (day, period); the
 * cell text comes from a caller-supplied function so each audience sees what
 * matters to them (head: subject/class/teacher, parent: subject).
 */
public final class TimetableGrid {

    private TimetableGrid() {
    }

    public static void render(List<String> periods, List<TimetableSlot> slots,
                              Function<TimetableSlot, String> cellText) {
        Map<DayOfWeek, Map<Integer, String>> grid = new LinkedHashMap<>();
        for (TimetableSlot s : slots) {
            grid.computeIfAbsent(s.getDay(), d -> new LinkedHashMap<>())
                    .put(s.getPeriod(), cellText.apply(s));
        }
        String[] headers = new String[DayOfWeek.values().length + 1];
        headers[0] = "Period";
        for (DayOfWeek d : DayOfWeek.values()) {
            headers[d.ordinal() + 1] = d.name().charAt(0) + d.name().substring(1).toLowerCase();
        }
        List<String[]> rows = new ArrayList<>();
        for (int p = 1; p <= periods.size(); p++) {
            String[] row = new String[DayOfWeek.values().length + 1];
            row[0] = "P" + p + " " + periods.get(p - 1);
            for (DayOfWeek d : DayOfWeek.values()) {
                Map<Integer, String> byPeriod = grid.get(d);
                row[d.ordinal() + 1] = byPeriod == null ? "" : byPeriod.getOrDefault(p, "");
            }
            rows.add(row);
        }
        Console.table(headers, rows);
    }
}
