package com.cdb.cms.model;

/**
 * Approval lifecycle of proposed entities: classes, subjects and timetable slots.
 * The JS reference stores these as string fields ('pending'/'approved'/'rejected').
 */
public enum ProposalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
