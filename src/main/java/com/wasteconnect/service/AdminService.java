package com.wasteconnect.service;

import com.wasteconnect.dao.*;
import com.wasteconnect.engine.BenchmarkEngine;
import com.wasteconnect.engine.HotspotDetector;
import com.wasteconnect.engine.RoutingEngine;
import com.wasteconnect.engine.SmartBinSimulator;
import com.wasteconnect.exception.ValidationException;
import com.wasteconnect.model.*;
import com.wasteconnect.pattern.PriorityStrategy;
import com.wasteconnect.pattern.RoutingStrategy;
import com.wasteconnect.pattern.EventBus;
import com.wasteconnect.pattern.UserFactory;
import com.wasteconnect.util.AppLogger;
import com.wasteconnect.util.CsvExporter;
import com.wasteconnect.util.Validators;
import com.wasteconnect.util.PasswordHasher;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.*;

/**
 * Service managing municipal administrator operations, fleet scheduling, SLA escalation,
 * macro analytics, and CSV reporting.
 */
public class AdminService {
    private final ZoneDao zoneDao;
    private final BinDao binDao;
    private final VehicleDao vehicleDao;
    private final CollectorDao collectorDao;
    private final HouseholdDao householdDao;
    private final ComplaintDao complaintDao;
    private final WastePickerDao wastePickerDao;
    private final RouteDao routeDao;
    private final SegregationLogDao segregationLogDao;
    private final SettingDao settingDao;

    private final RoutingEngine routingEngine;
    private final HotspotDetector hotspotDetector;
    private final BenchmarkEngine benchmarkEngine;
    private final SmartBinSimulator simulator;
    private final NotificationService notificationService;

    public AdminService() {
        this(new ZoneDaoImpl(), new BinDaoImpl(), new VehicleDaoImpl(),
                new CollectorDaoImpl(), new HouseholdDaoImpl(), new ComplaintDaoImpl(),
                new WastePickerDaoImpl(), new RouteDaoImpl(), new SegregationLogDaoImpl(),
                new SettingDaoImpl(), new NotificationService());
    }

    public AdminService(ZoneDao zoneDao, BinDao binDao, VehicleDao vehicleDao,
                        CollectorDao collectorDao, HouseholdDao householdDao,
                        ComplaintDao complaintDao, WastePickerDao wastePickerDao,
                        RouteDao routeDao, SegregationLogDao segregationLogDao,
                        SettingDao settingDao, NotificationService notificationService) {
        this.zoneDao = zoneDao;
        this.binDao = binDao;
        this.vehicleDao = vehicleDao;
        this.collectorDao = collectorDao;
        this.householdDao = householdDao;
        this.complaintDao = complaintDao;
        this.wastePickerDao = wastePickerDao;
        this.routeDao = routeDao;
        this.segregationLogDao = segregationLogDao;
        this.settingDao = settingDao;
        this.notificationService = notificationService;

        this.routingEngine = new RoutingEngine(binDao, new com.wasteconnect.engine.PriorityScoreEngine(settingDao));
        this.hotspotDetector = new HotspotDetector(binDao, complaintDao, zoneDao);
        this.benchmarkEngine = new BenchmarkEngine();
        this.simulator = new SmartBinSimulator(binDao, zoneDao);
    }

    // --- ZONES CRUD ---
    public List<Zone> getAllZones() {
        return zoneDao.findAll();
    }

    public Zone saveZone(Zone zone) {
        Validators.validateNonBlank(zone.getName(), "Zone Name");
        Validators.validateNonBlank(zone.getCode(), "Zone Code");
        Validators.validateCoordinates(zone.getCenterLat(), zone.getCenterLng());
        return zoneDao.save(zone);
    }

    public boolean updateZone(Zone zone) {
        Validators.validateNonBlank(zone.getName(), "Zone Name");
        return zoneDao.update(zone);
    }

    public boolean deleteZone(String zoneId) {
        return zoneDao.deleteById(zoneId);
    }

    // --- BINS CRUD & PAGINATION ---
    public List<Bin> getBinsPage(int page, int pageSize, String zoneId, String wasteType, String search) {
        int offset = Math.max(0, page) * pageSize;
        return binDao.findPage(offset, pageSize, zoneId, wasteType, search);
    }

    public int getBinsCount(String zoneId, String wasteType, String search) {
        return binDao.count(zoneId, wasteType, search);
    }

    public Bin saveBin(Bin bin) {
        if (bin == null) {
            throw new ValidationException("Bin details are required.");
        }
        Validators.validateNonBlank(bin.getBinCode(), "Bin Code");
        Validators.validateNonBlank(bin.getZoneId(), "Zone");
        Validators.validateNonBlank(bin.getLocation().getAddress(), "Bin location");
        Validators.validateCoordinates(bin.getLocation().getLatitude(), bin.getLocation().getLongitude());
        if (zoneDao.findById(bin.getZoneId()).isEmpty()) {
            throw new ValidationException("Zone not found: " + bin.getZoneId());
        }
        return binDao.save(bin);
    }

    public boolean updateBin(Bin bin) {
        if (bin == null) {
            throw new ValidationException("Bin details are required.");
        }
        Validators.validateNonBlank(bin.getBinCode(), "Bin Code");
        Validators.validateNonBlank(bin.getZoneId(), "Zone");
        Validators.validateCoordinates(bin.getLocation().getLatitude(), bin.getLocation().getLongitude());
        if (zoneDao.findById(bin.getZoneId()).isEmpty()) {
            throw new ValidationException("Zone not found: " + bin.getZoneId());
        }
        return binDao.update(bin);
    }

    public boolean deleteBin(String binId) {
        Validators.validateNonBlank(binId, "Bin ID");
        return binDao.deleteById(binId);
    }

    // --- VEHICLES CRUD ---
    public List<Vehicle> getAllVehicles() {
        return vehicleDao.findAll();
    }

    public Vehicle saveVehicle(Vehicle vehicle) {
        Validators.validateNonBlank(vehicle.getRegistrationNumber(), "Registration Number");
        return vehicleDao.save(vehicle);
    }

    public boolean updateVehicle(Vehicle vehicle) {
        return vehicleDao.update(vehicle);
    }

    public boolean deleteVehicle(String vehicleId) {
        return vehicleDao.deleteById(vehicleId);
    }

    // --- COLLECTORS CRUD ---
    public List<Collector> getAllCollectors() {
        return collectorDao.findAll();
    }

    public Collector saveCollector(String name, String email, String password,
                                   String employeeId, String zoneId, String vehicleId) {
        Validators.validateNonBlank(name, "Name");
        Validators.validateEmail(email);
        Validators.validatePasswordStrength(password);
        Validators.validateNonBlank(employeeId, "Employee ID");
        Validators.validateNonBlank(zoneId, "Zone");
        Validators.validateNonBlank(vehicleId, "Vehicle");
        if (new UserDaoImpl().findByEmail(email).isPresent()) {
            throw new ValidationException("An account already exists with email: " + email);
        }
        if (zoneDao.findById(zoneId).isEmpty()) {
            throw new ValidationException("Zone not found: " + zoneId);
        }
        if (vehicleDao.findById(vehicleId).isEmpty()) {
            throw new ValidationException("Vehicle not found: " + vehicleId);
        }
        if (collectorDao.findByEmployeeId(employeeId).isPresent()) {
            throw new ValidationException("An account already exists with employee ID: " + employeeId);
        }
        Collector collector = UserFactory.createCollector(name, email, PasswordHasher.hashPassword(password),
                employeeId, zoneId, vehicleId);
        collectorDao.save(collector);
        return collector;
    }

    public boolean updateCollector(Collector collector) {
        Validators.validateNonBlank(collector.getName(), "Name");
        Validators.validateEmail(collector.getEmail());
        Validators.validateNonBlank(collector.getEmployeeId(), "Employee ID");
        new UserDaoImpl().findByEmail(collector.getEmail()).filter(user -> !user.getId().equals(collector.getId()))
            .ifPresent(user -> { throw new ValidationException("An account already exists with email: " + collector.getEmail()); });
        collectorDao.findByEmployeeId(collector.getEmployeeId()).filter(existing -> !existing.getId().equals(collector.getId()))
            .ifPresent(existing -> { throw new ValidationException("An account already exists with employee ID: " + collector.getEmployeeId()); });
        if (collector.getAssignedZoneId() != null && zoneDao.findById(collector.getAssignedZoneId()).isEmpty()) {
            throw new ValidationException("Assigned zone was not found.");
        }
        if (collector.getAssignedVehicleId() != null && vehicleDao.findById(collector.getAssignedVehicleId()).isEmpty()) {
            throw new ValidationException("Assigned vehicle was not found.");
        }
        return collectorDao.update(collector);
    }

    public boolean deleteCollector(String collectorId) {
        return collectorDao.deleteById(collectorId);
    }

    // --- HOUSEHOLDS PAGINATION ---
    public List<Household> getHouseholdsPage(int page, int pageSize, String search) {
        int offset = Math.max(0, page) * pageSize;
        return householdDao.findPage(offset, pageSize, search);
    }

    public int getHouseholdsCount(String search) {
        return householdDao.count(search);
    }
    public Household saveHousehold(Household household) {
        validateHousehold(household);
        return householdDao.save(household);
    }
    
    public boolean updateHousehold(Household household) {
        validateHousehold(household);
        return householdDao.update(household);
    }
    
    public boolean deleteHousehold(String householdId) {
        Validators.validateNonBlank(householdId, "Household ID");
        return householdDao.deleteById(householdId);
    }
    
    private void validateHousehold(Household household) {
        if (household == null) {
            throw new ValidationException("Household details are required.");
        }
        Validators.validateNonBlank(household.getId(), "Household ID");
        Validators.validateNonBlank(household.getZoneId(), "Zone");
        Validators.validateNonBlank(household.getAddress(), "Address");
        Validators.validateCoordinates(household.getLocation().getLatitude(), household.getLocation().getLongitude());
        if (zoneDao.findById(household.getZoneId()).isEmpty()) {
            throw new ValidationException("Zone not found: " + household.getZoneId());
        }
    }

    public List<Route> getRoutesForZone(String zoneId, LocalDate routeDate) {
        return routeDao.findByZoneAndDate(zoneId, routeDate);
    }

    // --- ROUTE PLANNING & DISPATCH ---
    public Route planRoute(String zoneId, String vehicleId, String collectorId, RoutingStrategy strategy) {
        Zone zone = zoneDao.findById(zoneId)
                .orElseThrow(() -> new ValidationException("Zone not found: " + zoneId));
        Vehicle vehicle = vehicleDao.findById(vehicleId)
                .orElseThrow(() -> new ValidationException("Vehicle not found: " + vehicleId));

        Route route = routingEngine.plan(zone, vehicle, LocalDate.now(), strategy);
        route.setCollectorId(collectorId);
        routeDao.save(route);
        EventBus.getInstance().publish(new EventBus.RouteGenerated(route.getId(), zoneId, collectorId));

        if (collectorId != null) {
            notificationService.sendNotification(collectorId, "New Route Assigned",
                    String.format("Route %s for %s (%d stops, %.1f km) assigned for today.",
                            route.getId(), zone.getName(), route.getStopsCount(), route.getTotalDistanceKm()));
        }
        AppLogger.info("Admin generated and assigned route " + route.getId() + " to " + collectorId);
        return route;
    }

    // --- COMPLAINT SLA MONITORING & AUTO-ESCALATION ---
    public List<Complaint> getAllComplaints() {
        return complaintDao.findAll();
    }

    public String assignComplaintToZoneCollector(String complaintId) {
        Complaint complaint = complaintDao.findById(complaintId)
                .orElseThrow(() -> new ValidationException("Complaint not found: " + complaintId));
        List<Collector> collectors = collectorDao.findByZone(complaint.getZoneId());
        if (collectors.isEmpty()) {
            throw new ValidationException("No collector is assigned to zone " + complaint.getZoneId());
        }
        String collectorId = collectors.get(0).getId();
        if (!complaintDao.assignCollector(complaintId, collectorId)) {
            throw new ValidationException("Complaint assignment could not be saved.");
        }
        EventBus.getInstance().publish(new EventBus.ComplaintAssigned(complaintId, collectorId));
        notificationService.sendNotification(collectorId, "Complaint Assigned",
                "Complaint " + complaintId + " has been assigned to your zone queue.");
        return collectorId;
    }

    public boolean updateComplaintStatus(String complaintId, ComplaintStatus nextStatus, String remarks) {
        Validators.validateNonBlank(complaintId, "Complaint ID");
        if (nextStatus == null || nextStatus == ComplaintStatus.OPEN || nextStatus == ComplaintStatus.ASSIGNED) {
            throw new ValidationException("Select In Progress, Resolved, or Escalated as the next status.");
        }
        Complaint complaint = complaintDao.findById(complaintId)
                .orElseThrow(() -> new ValidationException("Complaint not found: " + complaintId));
        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            throw new ValidationException("A resolved complaint cannot be changed.");
        }
        Validators.validateNonBlank(remarks, "Status note");
        boolean updated = complaintDao.updateStatus(complaintId, nextStatus, remarks);
        if (!updated) throw new ValidationException("Complaint status was not updated.");

        ComplaintEvent event = new ComplaintEvent("EVT-" + UUID.randomUUID().toString().substring(0, 8),
                complaintId, complaint.getStatus(), nextStatus, remarks);
        complaintDao.logEvent(event);
        if (nextStatus == ComplaintStatus.RESOLVED) {
            EventBus.getInstance().publish(new EventBus.ComplaintResolved(complaintId, complaint.getCitizenId()));
            notificationService.sendNotification(complaint.getCitizenId(), "Complaint Resolved",
                    "Your complaint #" + complaintId + " has been resolved: " + remarks);
        } else if (nextStatus == ComplaintStatus.ESCALATED) {
            EventBus.getInstance().publish(new EventBus.ComplaintEscalated(complaintId, complaint.getZoneId()));
            notificationService.sendNotification(complaint.getCitizenId(), "Complaint Escalated",
                    "Your complaint #" + complaintId + " has been escalated: " + remarks);
        }
        AppLogger.info("Admin changed complaint " + complaintId + " from " + complaint.getStatus() + " to " + nextStatus);
        return true;
    }

    public List<Complaint> getSlaBreachedComplaints() {
        return complaintDao.findSlaBreached();
    }

    /**
     * Scans for complaints breaching SLA thresholds, auto-escalates them, and auto-assigns
     * to the nearest available collector in that zone.
     */
    public int escalateOverdueComplaints() {
        List<Complaint> breached = complaintDao.findSlaBreached();
        int count = 0;

        for (Complaint c : breached) {
            c.escalate();
            // Find nearest collector in the same zone
            List<Collector> zoneCollectors = collectorDao.findByZone(c.getZoneId());
            String autoAssignId = zoneCollectors.isEmpty() ? null : zoneCollectors.get(0).getId();

            complaintDao.updateStatus(c.getId(), ComplaintStatus.ESCALATED, "Auto-escalated due to SLA breach.");
            EventBus.getInstance().publish(new EventBus.ComplaintEscalated(c.getId(), c.getZoneId()));
            if (autoAssignId != null) {
                complaintDao.assignCollector(c.getId(), autoAssignId);
                EventBus.getInstance().publish(new EventBus.ComplaintAssigned(c.getId(), autoAssignId));
                notificationService.sendNotification(autoAssignId, "URGENT: Escalated Complaint Assigned",
                        "Complaint #" + c.getId() + " (" + c.getComplaintType().getLabel() + ") has breached SLA and is assigned to you.");
            }

            ComplaintEvent event = new ComplaintEvent("EVT-" + UUID.randomUUID().toString().substring(0, 8),
                    c.getId(), ComplaintStatus.OPEN, ComplaintStatus.ESCALATED, "System auto-escalated past SLA threshold.");
            complaintDao.logEvent(event);
            count++;
        }
        if (count > 0) {
            AppLogger.warning("Auto-escalated " + count + " SLA-breached complaints.");
        }
        return count;
    }

    // --- WASTE PICKER VERIFICATION ---
    public List<WastePicker> getUnverifiedPickers() {
        return wastePickerDao.findPendingVerification();
    }

    public List<WastePicker> getAllPickers() {
        return wastePickerDao.findAll();
    }

    public boolean verifyWastePicker(String pickerId, boolean approved) {
        boolean success = wastePickerDao.setVerificationStatus(pickerId, approved);
        if (success) {
            EventBus.getInstance().publish(new EventBus.PickerVerified(pickerId, approved));
            notificationService.sendNotification(pickerId, "Account Verification Update",
                    approved ? "Congratulations! Your WastePicker account has been verified by the municipality."
                            : "Your waste picker application was rejected. Please visit the municipal office.");
            AppLogger.info("Waste picker " + pickerId + " verification updated: " + approved);
        }
        return success;
    }

    // --- HOTSPOT ENGINE ---
    public List<HotspotReport> detectHotspots() {
        return hotspotDetector.detectHotspots();
    }

    // --- BENCHMARK ENGINE ---
    public BenchmarkEngine.BenchmarkComparison runBenchmark(String zoneId) {
        Zone zone = zoneDao.findById(zoneId)
                .orElse(zoneDao.findAll().get(0));
        List<Bin> bins = binDao.findByZone(zone.getId());
        Vehicle vehicle = vehicleDao.findAll().stream().filter(v -> v instanceof LargeTruck).findFirst()
                .orElse(vehicleDao.findAll().get(0));

        return benchmarkEngine.runBenchmark(zone, bins, vehicle);
    }

    // --- SIMULATOR CONTROLS ---
    public SmartBinSimulator getSimulator() {
        return simulator;
    }

    // --- MACRO ANALYTICS ---
    public WasteAnalytics getMunicipalAnalytics() {
        List<SegregationLog> logs = segregationLogDao.findRecent(1000);
        List<Complaint> complaints = complaintDao.findAll();
        List<Route> routes = routeDao.findAll();
        Map<String, Bin> binsById = new HashMap<>();
        for (Bin bin : binDao.findAll()) {
            binsById.put(bin.getId(), bin);
        }

        double totalWet = 0.0, totalDry = 0.0, totalHaz = 0.0, totalEWaste = 0.0;
        double goodCount = 0.0;

        for (SegregationLog log : logs) {
            if (log.getQuality() == SegregationQuality.GOOD) goodCount++;
            Bin bin = binsById.get(log.getBinId());
            if (bin == null) continue;
            switch (bin.getWasteType()) {
                case WET -> totalWet += log.getWeightKg();
                case DRY -> totalDry += log.getWeightKg();
                case HAZARDOUS -> totalHaz += log.getWeightKg();
                case E_WASTE -> totalEWaste += log.getWeightKg();
            }
        }

        double segregationRate = logs.isEmpty() ? 0.0 : Math.round((goodCount / logs.size() * 100.0) * 10.0) / 10.0;

        long resolvedComplaints = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();
        double resolvedPct = complaints.isEmpty() ? 0.0 : Math.round(((double) resolvedComplaints / complaints.size() * 100.0) * 10.0) / 10.0;

        double totalDist = routes.stream().mapToDouble(Route::getTotalDistanceKm).sum();
        long plannedStops = routes.stream().mapToLong(Route::getStopsCount).sum();
        long completedStops = routes.stream().mapToLong(Route::getCompletedStopsCount).sum();
        double collectionEfficiency = plannedStops == 0 ? 0.0
                : Math.round((double) completedStops / plannedStops * 1000.0) / 10.0;

        return new WasteAnalytics(
                Math.round(totalWet * 10.0) / 10.0,
                Math.round(totalDry * 10.0) / 10.0,
                Math.round(totalHaz * 10.0) / 10.0,
                Math.round(totalEWaste * 10.0) / 10.0,
                collectionEfficiency,
                segregationRate,
                resolvedPct,
                0.0,
                0.0
        );
    }

    // --- CSV EXPORT (CO4) ---
    public File exportBinsReport(File targetFile) throws IOException {
        return CsvExporter.export(binDao.findAll(), targetFile);
    }

    public File exportComplaintsReport(File targetFile) throws IOException {
        return CsvExporter.export(complaintDao.findAll(), targetFile);
    }

    public File exportHouseholdsReport(File targetFile) throws IOException {
        List<String[]> rows = householdDao.findAll().stream().map(household -> new String[]{
                household.getId(), household.getZoneId(), household.getCitizenId(), household.getAddress(),
                Double.toString(household.getLocation().getLatitude()),
                Double.toString(household.getLocation().getLongitude())}).toList();
        return CsvExporter.exportRaw(new String[]{"HouseholdId", "ZoneId", "CitizenId", "Address", "Latitude", "Longitude"}, rows, targetFile);
    }

    public File exportPointsLedgerReport(File targetFile) throws IOException {
        List<String[]> rows = new RewardTransactionDaoImpl().findAll().stream().map(transaction -> new String[]{
                transaction.getId(), transaction.getCitizenId(), transaction.getType().name(),
                Integer.toString(transaction.getPoints()), transaction.getDescription(),
                transaction.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}).toList();
        return CsvExporter.exportRaw(new String[]{"TransactionId", "CitizenId", "Type", "Points", "Description", "Timestamp"}, rows, targetFile);
    }
}
