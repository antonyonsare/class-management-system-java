package com.cdb.cms.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Labelled counters for the superadmin and school-head dashboards. Stored as an
 * ordered map so consoles can render it as a simple two-column table without
 * any presentation logic in the service layer.
 */
public class DashboardStats {

    private final Map<String, Long> counts = new LinkedHashMap<>();

    public DashboardStats add(String label, long value) {
        counts.put(label, value);
        return this;
    }

    public Map<String, Long> getCounts() {
        return counts;
    }
}
