package com.cdb.cms.service;

/**
 * Raised when a business rule is violated (duplicate email, unapproved
 * account, mark out of range, locked timetable, ...). Console layers catch it
 * and print the message; services never print and never return null for
 * expected failures.
 */
public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }
}
