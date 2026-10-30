package com.wasteconnect.dao;

import com.wasteconnect.model.Route;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RouteDao extends Repository<Route, String> {
    Optional<Route> findByCollectorAndDate(String collectorId, LocalDate date);
    List<Route> findByZoneAndDate(String zoneId, LocalDate date);
    boolean updateStopCollected(String routeId, int sequence, boolean collected);
    boolean updateRouteStatus(String routeId, String status);
}
