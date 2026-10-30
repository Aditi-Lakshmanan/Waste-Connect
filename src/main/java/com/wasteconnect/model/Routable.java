package com.wasteconnect.model;

/**
 * ABSTRACTION: Interface representing any geographical entity that can be visited along a route.
 */
public interface Routable {
    String getId();
    Location getLocation();
    double getPriorityScore();
    double getEstimatedVolume();
}
