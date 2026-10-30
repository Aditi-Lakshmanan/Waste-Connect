package com.wasteconnect.engine;

import com.wasteconnect.dao.BinDao;
import com.wasteconnect.dao.BinDaoImpl;
import com.wasteconnect.model.*;
import com.wasteconnect.pattern.PriorityStrategy;
import com.wasteconnect.pattern.RoutingStrategy;
import com.wasteconnect.util.AppLogger;

import java.time.LocalDate;
import java.util.*;

/**
 * Novelty Engine & Dijkstra Shortest-Path Graph Engine:
 * Plans optimized collection routes using pluggable routing strategies.
 * POLYMORPHISM (Compile-time): Features method overloading across multiple signatures.
 */
public class RoutingEngine {
    private final BinDao binDao;
    private final PriorityScoreEngine priorityEngine;

    public RoutingEngine() {
        this(new BinDaoImpl(), new PriorityScoreEngine());
    }

    public RoutingEngine(BinDao binDao, PriorityScoreEngine priorityEngine) {
        this.binDao = binDao;
        this.priorityEngine = priorityEngine;
    }

    // POLYMORPHISM (Compile-Time / Overloading): plan(Zone)
    public Route plan(Zone zone) {
        return plan(zone, null, LocalDate.now(), new PriorityStrategy());
    }

    // POLYMORPHISM (Compile-Time / Overloading): plan(Zone, Vehicle)
    public Route plan(Zone zone, Vehicle vehicle) {
        return plan(zone, vehicle, LocalDate.now(), new PriorityStrategy());
    }

    // POLYMORPHISM (Compile-Time / Overloading): plan(Zone, Vehicle, LocalDate)
    public Route plan(Zone zone, Vehicle vehicle, LocalDate date) {
        return plan(zone, vehicle, date, new PriorityStrategy());
    }

    // POLYMORPHISM (Compile-Time / Overloading): plan(Zone, Vehicle, LocalDate, RoutingStrategy)
    public Route plan(Zone zone, Vehicle vehicle, LocalDate date, RoutingStrategy strategy) {
        if (zone == null) {
            throw new IllegalArgumentException("Zone cannot be null for route planning.");
        }
        if (strategy == null) {
            strategy = new PriorityStrategy();
        }

        List<Bin> zoneBins = binDao.findByZone(zone.getId());
        for (Bin bin : zoneBins) {
            priorityEngine.calculatePriority(bin);
        }

        Location depot = zone.getCenterLocation();
        List<RouteStop> stops = strategy.calculateRoute(zone, zoneBins, vehicle, depot);
        double totalDistanceKm = strategy.calculateTotalDistance(stops, depot);

        String routeId = "RT-" + zone.getCode() + "-" + UUID.randomUUID().toString().substring(0, 6);
        Route route = new Route(routeId, zone.getId(), null, vehicle != null ? vehicle.getId() : "V1", date, strategy.getStrategyName());
        route.setStops(stops);
        route.setTotalDistanceKm(totalDistanceKm);

        // Average transit speed: 25 km/h + 3 minutes per stop
        double transitHours = totalDistanceKm / 25.0;
        double collectionHours = (stops.size() * 3.0) / 60.0;
        route.setEstimatedTimeHours(Math.round((transitHours + collectionHours) * 10.0) / 10.0);

        AppLogger.info(String.format("Planned %s route for Zone %s: %d stops, %.2f km, %.1f hours.",
                strategy.getStrategyName(), zone.getName(), stops.size(), totalDistanceKm, route.getEstimatedTimeHours()));

        return route;
    }

    /**
     * Dijkstra's Shortest Path Algorithm on a weighted graph of geographical locations.
     * Computes the minimal travel distance between two points given a set of intermediate nodes.
     */
    public static double dijkstraShortestPath(Location source, Location target, List<Location> intermediateNodes) {
        if (source == null || target == null) return 0.0;
        if (source.equals(target)) return 0.0;

        List<Location> allNodes = new ArrayList<>();
        allNodes.add(source);
        if (intermediateNodes != null) {
            for (Location loc : intermediateNodes) {
                if (!loc.equals(source) && !loc.equals(target)) {
                    allNodes.add(loc);
                }
            }
        }
        allNodes.add(target);

        int n = allNodes.size();
        int srcIdx = 0;
        int tgtIdx = n - 1;

        double[] dist = new double[n];
        Arrays.fill(dist, Double.MAX_VALUE);
        dist[srcIdx] = 0.0;

        // CO4: PriorityQueue storing (nodeIndex, currentDistance)
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance));
        pq.add(new NodeDistance(srcIdx, 0.0));
        boolean[] visited = new boolean[n];

        while (!pq.isEmpty()) {
            NodeDistance curr = pq.poll();
            int u = curr.nodeIndex;
            if (visited[u]) continue;
            visited[u] = true;

            if (u == tgtIdx) {
                return dist[tgtIdx];
            }

            for (int v = 0; v < n; v++) {
                if (u != v && !visited[v]) {
                    double weight = allNodes.get(u).distanceTo(allNodes.get(v));
                    if (dist[u] + weight < dist[v]) {
                        dist[v] = dist[u] + weight;
                        pq.add(new NodeDistance(v, dist[v]));
                    }
                }
            }
        }
        return dist[tgtIdx];
    }

    private static class NodeDistance {
        int nodeIndex;
        double distance;

        NodeDistance(int nodeIndex, double distance) {
            this.nodeIndex = nodeIndex;
            this.distance = distance;
        }
    }
}
