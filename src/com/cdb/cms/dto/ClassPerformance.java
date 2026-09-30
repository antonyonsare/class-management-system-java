package com.cdb.cms.dto;

/**
 * A class's mean average compared against the school mean average for the same
 * term — the comparison shown on the school head's analytics page.
 */
public class ClassPerformance {

    private final String classId;
    private final String className;
    private final double classAverage;
    private final double schoolAverage;

    public ClassPerformance(String classId, String className,
                            double classAverage, double schoolAverage) {
        this.classId = classId;
        this.className = className;
        this.classAverage = classAverage;
        this.schoolAverage = schoolAverage;
    }

    public String getClassId() {
        return classId;
    }

    public String getClassName() {
        return className;
    }

    public double getClassAverage() {
        return classAverage;
    }

    public double getSchoolAverage() {
        return schoolAverage;
    }

    /** Signed gap of this class above (+) or below (-) the school average. */
    public double gap() {
        return classAverage - schoolAverage;
    }
}
