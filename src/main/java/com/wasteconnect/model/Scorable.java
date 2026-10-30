package com.wasteconnect.model;

/**
 * ABSTRACTION: Interface representing entities evaluated by a quantitative rating or scoring engine.
 */
public interface Scorable {
    double calculateScore();
    double getScore();
}
