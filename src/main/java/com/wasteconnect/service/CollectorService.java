package com.wasteconnect.service;

import com.wasteconnect.dao.*;
import com.wasteconnect.exception.ValidationException;
import com.wasteconnect.model.*;
import com.wasteconnect.pattern.PriorityStrategy;
import com.wasteconnect.pattern.RoutingStrategy;
import com.wasteconnect.pattern.EventBus;
import com.wasteconnect.util.AppLogger;
import com.wasteconnect.util.Validators;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing Collector field operations, vehicle route execution, stop logging,
 * and dynamic contingency re-routing.
 */
public class CollectorService {
    private final RouteDao routeDao;
    private final BinDao binDao;
    private final CitizenDao citizenDao;
    private final SegregationLogDao segregationLogDao;
    private final RewardTransactionDao rewardTransactionDao;
    private final ComplaintDao complaintDao;
    private final CollectorDao collectorDao;
    private final VehicleDao vehicleDao;
    private final ZoneDao zoneDao;
    private final NotificationService notificationService;

    public CollectorService() {
        this(new RouteDaoImpl(), new BinDaoImpl(), new CitizenDaoImpl(),
                new SegregationLogDaoImpl(), new RewardTransactionDaoImpl(),
                new ComplaintDaoImpl(), new CollectorDaoImpl(), new VehicleDaoImpl(),
                new ZoneDaoImpl(), new NotificationService());
    }

    public CollectorService(RouteDao routeDao, BinDao binDao, CitizenDao citizenDao,
                            SegregationLogDao segregationLogDao, RewardTransactionDao rewardTransactionDao,
                            ComplaintDao complaintDao, CollectorDao collectorDao, VehicleDao vehicleDao,
                            ZoneDao zoneDao, NotificationService notificationService) {
        this.routeDao = routeDao;
        this.binDao = binDao;
        this.citizenDao = citizenDao;
        this.segregationLogDao = segregationLogDao;
        this.rewardTransactionDao = rewardTransactionDao;
        this.complaintDao = complaintDao;
        this.collectorDao = collectorDao;
        this.vehicleDao = vehicleDao;
        this.zoneDao = zoneDao;
        this.notificationService = notificationService;
    }

    public Optional<Route> getTodayRoute(String collectorId) {
        return routeDao.findByCollectorAndDate(collectorId, LocalDate.now());
    }

    public Optional<Collector> getCollector(String collectorId) {
        return collectorDao.findById(collectorId);
    }

    /**
     * CO3: Atomic execution when a stop is collected.
     * 1. Marks RouteStop collected
     * 2. Empties the Bin and resets fill percentage to 0
     * 3. Increments collector completed pickups count
     * 4. If associated with a citizen (e.g. household / door-to-door), records segregation log,
     *    updates citizen streak, rolling segregation score, and reward balance.
     */
    public boolean recordStopCollected(String routeId, int stopSequence, String binId,
                                       SegregationQuality quality, double actualWeightKg, String citizenId) {
        Validators.validatePositive(actualWeightKg, "Collected Weight");
        if (quality == null) throw new ValidationException("Segregation Quality must be graded.");

        int pointsDeltaForEvent = 0;
        int pointsBalanceForEvent = -1;
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                // 1. Update stop
                routeDao.updateStopCollected(routeId, stopSequence, true);

                // 2. Empty bin
                if (binId != null && !binId.isBlank()) {
                    binDao.recordCollection(binId);
                }

                // 3. Increment collector stats
                Route route = routeDao.findById(routeId).orElse(null);
                String collectorId = route != null ? route.getCollectorId() : "COL001";
                collectorDao.incrementPickups(collectorId);

                // 4. Update Citizen score and rewards if citizen identified
                if (citizenId != null && !citizenId.isBlank()) {
                    Optional<Citizen> citOpt = citizenDao.findById(citizenId);
                    if (citOpt.isPresent()) {
                        Citizen citizen = citOpt.get();
                        int oldPoints = citizen.getPointsBalance();
                        citizen.recordSegregationEvaluation(quality);

                        citizenDao.updateScoreAndPoints(citizenId, citizen.getSegregationScore(),
                                citizen.getStreak(), citizen.getPointsBalance());

                        // Log segregation
                        String sLogId = "SLOG-" + UUID.randomUUID().toString().substring(0, 8);
                        int ptsDelta = citizen.getPointsBalance() - oldPoints;
                        pointsDeltaForEvent = ptsDelta;
                        pointsBalanceForEvent = citizen.getPointsBalance();
                        SegregationLog log = new SegregationLog(sLogId, citizenId, collectorId, binId,
                                quality, actualWeightKg, ptsDelta);
                        segregationLogDao.save(log);

                        // Reward transaction
                        String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
                        RewardTransaction tx = new RewardTransaction(txId, citizenId,
                                ptsDelta >= 0 ? RewardTransaction.Type.EARNED : RewardTransaction.Type.PENALTY,
                                ptsDelta, "Segregation check: " + quality.getLabel());
                        rewardTransactionDao.save(tx);

                        notificationService.sendNotification(citizenId, "Collection Completed",
                                String.format("Waste collected (%.1f kg). Quality: %s (%+d pts). New streak: %d",
                                        actualWeightKg, quality.getLabel(), ptsDelta, citizen.getStreak()));
                    }
                }

                conn.commit();
                conn.setAutoCommit(autoCommit);
                EventBus.getInstance().publish(new EventBus.HouseholdCollected(
                        citizenId == null || citizenId.isBlank() ? binId : citizenId,
                        collectorId, routeId, stopSequence, actualWeightKg, quality));
                if (citizenId != null && !citizenId.isBlank() && pointsBalanceForEvent >= 0) {
                    EventBus.getInstance().publish(new EventBus.PointsAwarded(
                            citizenId, pointsDeltaForEvent, pointsBalanceForEvent));
                }
                AppLogger.info("Recorded stop collection for route " + routeId + " stop #" + stopSequence);
                return true;
            } catch (Exception ex) {
                conn.rollback();
                conn.setAutoCommit(autoCommit);
                AppLogger.severe("Transaction failed in recordStopCollected", ex);
                throw new RuntimeException("Failed to record stop collection: " + ex.getMessage(), ex);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database connection error", e);
        }
    }

    public List<Complaint> getAssignedComplaints(String collectorId) {
        return complaintDao.findByCollector(collectorId);
    }

    public boolean resolveComplaint(String complaintId, String resolutionNotes) {
        Validators.validateNonBlank(resolutionNotes, "Resolution Notes");
        boolean updated = complaintDao.updateStatus(complaintId, ComplaintStatus.RESOLVED, resolutionNotes);
        if (updated) {
            ComplaintEvent event = new ComplaintEvent("EVT-" + UUID.randomUUID().toString().substring(0, 8),
                    complaintId, ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED, resolutionNotes);
            complaintDao.logEvent(event);
                complaintDao.findById(complaintId).ifPresent(complaint -> EventBus.getInstance().publish(
                    new EventBus.ComplaintResolved(complaintId, complaint.getCitizenId())));

            complaintDao.findById(complaintId).ifPresent(c -> {
                notificationService.sendNotification(c.getCitizenId(), "Complaint Resolved",
                        "Your complaint #" + complaintId + " has been resolved: " + resolutionNotes);
            });
            AppLogger.info("Complaint " + complaintId + " resolved by collector.");
        }
        return updated;
    }

    /**
     * Reports a blocked road, obstacle or vehicle breakdown, which triggers automatic route re-planning.
     */
    public Route reportBlockedRoad(String routeId, int blockedSequence, String obstacleDescription) {
        Route route = routeDao.findById(routeId)
                .orElseThrow(() -> new ValidationException("Route not found: " + routeId));

        AppLogger.warning(String.format("Road obstruction reported on Route %s at stop #%d: %s. Re-planning remaining stops...",
                routeId, blockedSequence, obstacleDescription));

        // Remaining uncollected stops
        List<RouteStop> currentStops = route.getStops();
        List<Bin> remainingBins = new ArrayList<>();
        Location lastValidLocation = null;

        for (RouteStop s : currentStops) {
            if (s.getSequence() < blockedSequence && s.isCollected()) {
                lastValidLocation = s.getLocation();
            } else if (s.getSequence() != blockedSequence && !s.isCollected()) {
                binDao.findById(s.getStopId()).ifPresent(remainingBins::add);
            }
        }

        Zone zone = zoneDao.findById(route.getZoneId()).orElse(null);
        Vehicle vehicle = vehicleDao.findById(route.getVehicleId()).orElse(null);
        RoutingStrategy strategy = new PriorityStrategy(20.0);

        List<RouteStop> reCalculated = strategy.calculateRoute(zone, remainingBins, vehicle, lastValidLocation);

        // Re-assemble route with already collected stops + re-routed uncollected stops
        List<RouteStop> finalStops = new ArrayList<>();
        int seq = 1;
        for (RouteStop s : currentStops) {
            if (s.isCollected()) {
                finalStops.add(new RouteStop(seq++, s.getStopId(), s.getLocation(), s.getDemandKg(), s.getPriorityScore(), true, s.getNotes()));
            }
        }
        for (RouteStop s : reCalculated) {
            finalStops.add(new RouteStop(seq++, s.getStopId(), s.getLocation(), s.getDemandKg(), s.getPriorityScore(), false, "Re-routed stop: " + obstacleDescription));
        }

        route.setStops(finalStops);
        double newDist = strategy.calculateTotalDistance(finalStops, lastValidLocation);
        route.setTotalDistanceKm(newDist);
        routeDao.deleteById(route.getId());
        routeDao.save(route);
        EventBus.getInstance().publish(new EventBus.RouteReplanned(route.getId(),
            (int) finalStops.stream().filter(stop -> !stop.isCollected()).count()));

        AppLogger.info("Route " + routeId + " dynamically re-planned successfully with " + finalStops.size() + " stops.");
        return route;
    }
}
