package com.wasteconnect.model;

/**
 * ABSTRACTION: Interface providing standard CSV serialization capability for reporting.
 */
public interface Exportable {
    String[] getCsvHeaders();
    String toCsvRow();
}
