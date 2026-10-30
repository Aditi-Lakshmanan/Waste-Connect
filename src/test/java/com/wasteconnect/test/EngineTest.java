package com.wasteconnect.test;

import com.wasteconnect.dao.*;
import com.wasteconnect.engine.*;
import com.wasteconnect.model.*;
import com.wasteconnect.pattern.FixedScheduleStrategy;
import com.wasteconnect.pattern.PriorityStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class EngineTest {
    private PriorityScoreEngine priorityEngine;
    private RoutingEngine routingEngine;
    private Zone sampleZone;
    private List<Bin> sampleBins;

    @BeforeEach
    void setUp() {
        priorityEngine = new PriorityScoreEngine();
        routingEngine = new RoutingEngine();
        sampleZone = new Zone("Z1", "Commercial Ward", "CW", 12.9716, 77.5946, true);

        sampleBins = new ArrayList<>();
        // High urgency overflowing bin
        sampleBins.add(new Bin("B1", "BIN-001", "Z1",
                new Location(12.9720, 77.5950, "Corner 1"), WasteType.WET, 400.0,
                95.0, LocalDateTime.now().minusDays(2), BinStatus.WARNING, 2));

        // Low fill fresh bin
        sampleBins.add(new Bin("B2", "BIN-002", "Z1",
                new Location(12.9730, 77.5960, "Corner 2"), WasteType.DRY, 400.0,
                20.0, LocalDateTime.now().minusHours(4), BinStatus.NORMAL, 0));

        // Critical hazardous bin
        sampleBins.add(new Bin("B3", "BIN-003", "Z1",
                new Location(12.9740, 77.5970, "Hospital Corner"), WasteType.HAZARDOUS, 240.0,
                82.0, LocalDateTime.now().minusDays(1), BinStatus.WARNING, 1));
    }

    @Test
    @DisplayName("Engine: Priority score ranks urgent overflowing bins above empty bins")
    void testPriorityScoreCalculation() {
        Bin urgent = sampleBins.get(0);
        Bin low = sampleBins.get(1);

        double urgentScore = priorityEngine.calculatePriority(urgent);
        double lowScore = priorityEngine.calculatePriority(low);

        assertTrue(urgentScore > lowScore, "Urgent bin score must be higher than low-fill bin");
        assertTrue(urgentScore >= 50.0, "95% filled bin with complaints should have high score");
    }

    @Test
    @DisplayName("Engine: Dijkstra shortest path computes minimal travel distance")
    void testDijkstraAlgorithm() {
        Location src = new Location(12.9716, 77.5946, "Source");
        Location mid = new Location(12.9750, 77.5970, "Intermediate");
        Location tgt = new Location(12.9800, 77.6000, "Target");

        double directDist = src.distanceTo(tgt);
        double dijkstraDist = RoutingEngine.dijkstraShortestPath(src, tgt, List.of(mid));

        assertTrue(dijkstraDist > 0.0);
        assertEquals(directDist, dijkstraDist, 0.001, "Direct graph distance should match");
    }

    @Test
    @DisplayName("Engine: PriorityStrategy filters urgent stops while FixedSchedule includes all in sequence")
    void testRoutingStrategiesComparison() {
        Vehicle vehicle = new SmallVehicle("V1", "KA-01-101", 1500.0);
        Location depot = sampleZone.getCenterLocation();

        FixedScheduleStrategy fixed = new FixedScheduleStrategy();
        PriorityStrategy priority = new PriorityStrategy(40.0);

        List<RouteStop> fixedStops = fixed.calculateRoute(sampleZone, sampleBins, vehicle, depot);
        List<RouteStop> priorityStops = priority.calculateRoute(sampleZone, sampleBins, vehicle, depot);

        // Fixed includes B1, B2, B3 in alphabetical order
        assertEquals(3, fixedStops.size(), "Fixed strategy includes all stops");

        // Priority only selects high-priority bins above threshold
        assertTrue(priorityStops.size() >= 1);
        boolean containsB2 = priorityStops.stream().anyMatch(s -> s.getStopId().equals("B2"));
        assertFalse(containsB2, "Priority strategy should exclude low-fill bin B2");
    }

    @Test
    @DisplayName("Engine: BenchmarkEngine calculates side-by-side efficiency and missed pickups")
    void testBenchmarkEngine() {
        BenchmarkEngine be = new BenchmarkEngine();
        Vehicle vehicle = new LargeTruck("V1", "KA-01-TRK", 8000.0);

        BenchmarkEngine.BenchmarkComparison comparison = be.runBenchmark(sampleZone, sampleBins, vehicle);

        assertNotNull(comparison.fixedResult());
        assertNotNull(comparison.priorityResult());
        assertTrue(comparison.priorityResult().overflowIncidentsPrevented() >= 0);
        assertTrue(comparison.fixedResult().totalDistanceKm() > 0.0);
    }

    @Test
    @DisplayName("Engine: HotspotDetector identifies vulnerable zones and ranks by composite risk")
    void testHotspotDetector() {
        HotspotDetector detector = new HotspotDetector();
        List<HotspotReport> reports = detector.detectHotspots();

        assertNotNull(reports);
        if (!reports.isEmpty()) {
            HotspotReport top = reports.get(0);
            assertNotNull(top.zoneId());
            assertNotNull(top.riskLevel());
            assertNotNull(top.recommendation());
            assertTrue(top.riskScore() >= 0.0 && top.riskScore() <= 100.0);
        }
    }

    @Test
    @DisplayName("Engine: SmartBinSimulator manages lifecycle, speed multiplier, and festival surge")
    void testSmartBinSimulator() {
        SmartBinSimulator simulator = new SmartBinSimulator();
        assertFalse(simulator.isRunning());

        simulator.setSpeedMultiplier(3.5);
        assertEquals(3.5, simulator.getSpeedMultiplier(), 0.01);

        simulator.start();
        assertTrue(simulator.isRunning());

        simulator.triggerFestivalSurge();
        simulator.resetData();

        simulator.stop();
        assertFalse(simulator.isRunning());
    }
}
