package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.model.Location;
import com.wasteconnect.model.Route;
import com.wasteconnect.model.RouteStop;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RouteDaoImpl extends BaseDao<Route, String> implements RouteDao {

    @Override
    public Optional<Route> findById(String id) {
        String sql = "SELECT * FROM routes WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Route r = mapRoute(rs);
                    loadStopsForRoute(r);
                    return Optional.of(r);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query route by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Route> findByCollectorAndDate(String collectorId, LocalDate date) {
        String sql = "SELECT * FROM routes WHERE collector_id = ? AND route_date = ? ORDER BY created_at DESC, id DESC LIMIT 1;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, collectorId);
            pstmt.setString(2, date.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Route r = mapRoute(rs);
                    loadStopsForRoute(r);
                    return Optional.of(r);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query route for collector " + collectorId + " on " + date, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Route> findByZoneAndDate(String zoneId, LocalDate date) {
        List<Route> list = new ArrayList<>();
        String sql = "SELECT * FROM routes WHERE zone_id = ? AND route_date = ? ORDER BY id ASC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, zoneId);
            pstmt.setString(2, date.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Route r = mapRoute(rs);
                    loadStopsForRoute(r);
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query routes by zone and date", e);
        }
        return list;
    }

    @Override
    public List<Route> findAll() {
        List<Route> list = new ArrayList<>();
        String sql = "SELECT * FROM routes ORDER BY route_date DESC, id DESC;";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Route r = mapRoute(rs);
                loadStopsForRoute(r);
                list.add(r);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query all routes", e);
        }
        return list;
    }

    @Override
    public Route save(Route route) {
        Connection conn = getConnection();
        try {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                String rSql = "INSERT INTO routes (id, zone_id, collector_id, vehicle_id, route_date, created_at, status, total_distance_km, estimated_hours, strategy_used) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?);";
                try (PreparedStatement rPstmt = conn.prepareStatement(rSql)) {
                    rPstmt.setString(1, route.getId());
                    rPstmt.setString(2, route.getZoneId());
                    rPstmt.setString(3, route.getCollectorId());
                    rPstmt.setString(4, route.getVehicleId());
                    rPstmt.setString(5, route.getRouteDate().toString());
                    rPstmt.setString(6, route.getStatus());
                    rPstmt.setDouble(7, route.getTotalDistanceKm());
                    rPstmt.setDouble(8, route.getEstimatedTimeHours());
                    rPstmt.setString(9, route.getStrategyUsed());
                    rPstmt.executeUpdate();
                }

                // Batch insert stops
                String sSql = "INSERT INTO route_stops (id, route_id, sequence_num, stop_id, latitude, longitude, demand_kg, priority_score, collected, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
                try (PreparedStatement sPstmt = conn.prepareStatement(sSql)) {
                    for (RouteStop stop : route.getStops()) {
                        String stopPk = route.getId() + "-S" + stop.getSequence();
                        sPstmt.setString(1, stopPk);
                        sPstmt.setString(2, route.getId());
                        sPstmt.setInt(3, stop.getSequence());
                        sPstmt.setString(4, stop.getStopId());
                        sPstmt.setDouble(5, stop.getLocation().getLatitude());
                        sPstmt.setDouble(6, stop.getLocation().getLongitude());
                        sPstmt.setDouble(7, stop.getDemandKg());
                        sPstmt.setDouble(8, stop.getPriorityScore());
                        sPstmt.setInt(9, stop.isCollected() ? 1 : 0);
                        sPstmt.setString(10, stop.getNotes());
                        sPstmt.addBatch();
                    }
                    sPstmt.executeBatch();
                }

                conn.commit();
                conn.setAutoCommit(autoCommit);
                return route;
            } catch (SQLException ex) {
                conn.rollback();
                conn.setAutoCommit(autoCommit);
                throw ex;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save route: " + route.getId(), e);
        }
    }

    @Override
    public boolean update(Route route) {
        String sql = "UPDATE routes SET collector_id = ?, vehicle_id = ?, status = ?, total_distance_km = ?, estimated_hours = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, route.getCollectorId());
            pstmt.setString(2, route.getVehicleId());
            pstmt.setString(3, route.getStatus());
            pstmt.setDouble(4, route.getTotalDistanceKm());
            pstmt.setDouble(5, route.getEstimatedTimeHours());
            pstmt.setString(6, route.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update route: " + route.getId(), e);
        }
    }

    @Override
    public boolean updateStopCollected(String routeId, int sequence, boolean collected) {
        String sql = "UPDATE route_stops SET collected = ? WHERE route_id = ? AND sequence_num = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, collected ? 1 : 0);
            pstmt.setString(2, routeId);
            pstmt.setInt(3, sequence);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update stop collected status in route " + routeId, e);
        }
    }

    @Override
    public boolean updateRouteStatus(String routeId, String status) {
        String sql = "UPDATE routes SET status = ? WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, routeId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update route status: " + routeId, e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM routes WHERE id = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete route: " + id, e);
        }
    }

    private void loadStopsForRoute(Route route) {
        String sql = "SELECT * FROM route_stops WHERE route_id = ? ORDER BY sequence_num ASC;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, route.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                List<RouteStop> stops = new ArrayList<>();
                while (rs.next()) {
                    Location loc = new Location(rs.getDouble("latitude"), rs.getDouble("longitude"), "Stop " + rs.getString("stop_id"));
                    stops.add(new RouteStop(
                            rs.getInt("sequence_num"),
                            rs.getString("stop_id"),
                            loc,
                            rs.getDouble("demand_kg"),
                            rs.getDouble("priority_score"),
                            rs.getInt("collected") == 1,
                            rs.getString("notes")
                    ));
                }
                route.setStops(stops);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load stops for route: " + route.getId(), e);
        }
    }

    private Route mapRoute(ResultSet rs) throws SQLException {
        Route r = new Route(
                rs.getString("id"),
                rs.getString("zone_id"),
                rs.getString("collector_id"),
                rs.getString("vehicle_id"),
                LocalDate.parse(rs.getString("route_date")),
                rs.getString("strategy_used")
        );
        r.setStatus(rs.getString("status"));
        r.setTotalDistanceKm(rs.getDouble("total_distance_km"));
        r.setEstimatedTimeHours(rs.getDouble("estimated_hours"));
        return r;
    }
}
