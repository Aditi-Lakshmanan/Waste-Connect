package com.wasteconnect.test;

import com.wasteconnect.dao.*;
import com.wasteconnect.exception.ValidationException;
import com.wasteconnect.model.*;
import com.wasteconnect.service.AdminService;
import com.wasteconnect.service.CitizenService;
import com.wasteconnect.service.CollectorService;
import com.wasteconnect.pattern.PriorityStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class DaoAndServiceTest {

    @BeforeAll
    static void initDatabase() {
        // Run against the seeded test database instance
        DatabaseConnection.getInstance();
    }

    @AfterAll
    static void cleanup() {
        // keep connection clean
    }

    @Test
    @DisplayName("DAO: Citizen CRUD operations function correctly")
    void testCitizenDaoCrud() {
        CitizenDao citizenDao = new CitizenDaoImpl();

        Citizen citizen = new Citizen("TEST_CIT_01", "Test Resident", "test.resident@wasteconnect.com",
                "salt$hash123", "House 10, Test Lane", "+91-9999900001", "Z1");
        citizenDao.save(citizen);

        Optional<Citizen> found = citizenDao.findById("TEST_CIT_01");
        assertTrue(found.isPresent());
        assertEquals("Test Resident", found.get().getName());

        // Update
        citizen.setStreak(7);
        citizen.setSegregationScore(94.5);
        citizenDao.update(citizen);

        Optional<Citizen> updated = citizenDao.findById("TEST_CIT_01");
        assertTrue(updated.isPresent());
        assertEquals(7, updated.get().getStreak());
        assertEquals(94.5, updated.get().getSegregationScore(), 0.01);

        // Delete
        boolean deleted = citizenDao.deleteById("TEST_CIT_01");
        assertTrue(deleted);
        assertTrue(citizenDao.findById("TEST_CIT_01").isEmpty());
    }

    @Test
    @DisplayName("DAO: Bin CRUD and status filtering")
    void testBinDaoCrud() {
        BinDao binDao = new BinDaoImpl();
        Location loc = new Location(12.9716, 77.5946, "Test Corner");
        Bin bin = new Bin("TEST_BIN_01", "WC-TEST-001", "Z1", loc, WasteType.WET, 240.0,
                85.0, LocalDateTime.now(), BinStatus.WARNING, 1);

        binDao.save(bin);

        Optional<Bin> found = binDao.findById("TEST_BIN_01");
        assertTrue(found.isPresent());
        assertEquals("WC-TEST-001", found.get().getBinCode());

        // Record collection should reset fill level to 0 and status to NORMAL
        binDao.recordCollection("TEST_BIN_01");
        Bin collected = binDao.findById("TEST_BIN_01").get();
        assertEquals(0.0, collected.getCurrentFillPercentage(), 0.01);
        assertEquals(BinStatus.NORMAL, collected.getStatus());

        binDao.deleteById("TEST_BIN_01");
    }

    @Test
    @DisplayName("Service & CO3: Points redemption transaction rolls back on insufficient funds")
    void testPointsRedemptionTransactionRollback() {
        CitizenService citizenService = new CitizenService();

        // citizen1 has 50-200 points in seed data
        assertThrows(ValidationException.class, () -> {
            // Attempt to redeem 999,999 points (far exceeding balance)
            citizenService.redeemPoints("CIT001", 999999, "Luxury Solar Tax Rebate");
        });

        // Verify balance remained uncorrupted
        Citizen citizen = citizenService.getCitizen("CIT001").orElseThrow();
        assertTrue(citizen.getPointsBalance() < 999999);
    }

    @Test
    @DisplayName("Service & CO3: Successful points redemption deducts balance and records transaction")
    void testSuccessfulPointsRedemption() {
        CitizenService citizenService = new CitizenService();
        CitizenDao citizenDao = new CitizenDaoImpl();

        // Ensure user has at least 50 points
        citizenDao.updatePointsOnly("CIT002", 100);

        boolean redeemed = citizenService.redeemPoints("CIT002", 20, "Electricity Bill Rebate");
        assertTrue(redeemed);

        Citizen updated = citizenService.getCitizen("CIT002").orElseThrow();
        assertEquals(80, updated.getPointsBalance());

        List<RewardTransaction> txs = citizenService.getRewardTransactions("CIT002");
        assertFalse(txs.isEmpty());
        assertEquals(-20, txs.get(0).getPoints());
        assertTrue(txs.get(0).getDescription().matches(".*Code: WC-[A-F0-9]{8}$"));
    }

    @Test
    @DisplayName("Service: Raising complaint creates timeline event and notification")
    void testRaiseComplaintWorkflow() {
        CitizenService citizenService = new CitizenService();

        Complaint complaint = citizenService.raiseComplaint("CIT001", ComplaintType.MISSED_PICKUP,
                "Collector vehicle did not arrive today on 2nd cross", 12.972, 77.595, "Z1");

        assertNotNull(complaint.getId());
        assertEquals(ComplaintStatus.OPEN, complaint.getStatus());
        assertNotNull(complaint.getAssignedCollectorId(), "A collector in the complaint zone should be assigned automatically");
        assertTrue(new CollectorService().getAssignedComplaints(complaint.getAssignedCollectorId()).stream()
            .anyMatch(assigned -> assigned.getId().equals(complaint.getId())));

        List<ComplaintEvent> events = citizenService.getComplaintTimeline(complaint.getId());
        assertFalse(events.isEmpty(), "Initial OPEN event must be logged in timeline");
        assertEquals(ComplaintStatus.OPEN, events.get(0).getToStatus());
    }

    @Test
    @DisplayName("Service: Collector records stop collection and automatically updates citizen score and points")
    void testCollectorRecordStopUpdatesCitizen() {
        CollectorService collectorService = new CollectorService();
        CitizenDao citizenDao = new CitizenDaoImpl();

        citizenDao.updateScoreAndPoints("CIT003", 80.0, 3, 50);

        boolean logged = collectorService.recordStopCollected("RT-Z1-001", 1, "BIN0001",
                SegregationQuality.GOOD, 5.0, "CIT003");

        assertTrue(logged);
        Citizen updated = citizenDao.findById("CIT003").orElseThrow();

        // GOOD segregation adds +10 pts and increments streak
        assertEquals(60, updated.getPointsBalance(), "Good segregation should add +10 points");
        assertEquals(4, updated.getStreak(), "Streak should increment by 1");
        assertTrue(updated.getSegregationScore() > 80.0, "Rolling score should increase");
    }

    @Test
    @DisplayName("Service: Admin SLA escalation auto-escalates overdue complaints and auto-assigns collectors")
    void testAdminSlaEscalation() {
        AdminService adminService = new AdminService();
        ComplaintDao complaintDao = new ComplaintDaoImpl();

        String testCmpId = "CMP-OVD-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        complaintDao.deleteById(testCmpId);

        // Create an overdue complaint
        Complaint overdue = new Complaint(testCmpId, "CIT001", ComplaintType.OVERFLOWING_BIN,
                "Overdue overflow", new Location(12.972, 77.595, "Site"), "Z1");
        overdue.setCreatedAt(LocalDateTime.now().minusHours(10)); // 10h old, SLA is 2h
        complaintDao.save(overdue);

        int escalatedCount = adminService.escalateOverdueComplaints();
        assertTrue(escalatedCount >= 1, "At least one overdue complaint should be escalated");

        Complaint updated = complaintDao.findById(testCmpId).orElseThrow();
        assertEquals(ComplaintStatus.ESCALATED, updated.getStatus());
        assertNotNull(updated.getAssignedCollectorId(), "Collector should be auto-assigned");

        complaintDao.deleteById(testCmpId);
        complaintDao.deleteById("CMP-TEST-OVERDUE");
    }

    @Test
    @DisplayName("Service: Admin-generated route is persisted and visible to its collector")
    void testGeneratedRouteIsVisibleToCollector() {
        AdminService adminService = new AdminService();
        CollectorService collectorService = new CollectorService();
        RouteDao routeDao = new RouteDaoImpl();

        Route route = adminService.planRoute("Z1", "V1", "COL001", new PriorityStrategy());
        try {
            assertTrue(adminService.getRoutesForZone("Z1", LocalDate.now()).stream()
                    .anyMatch(savedRoute -> savedRoute.getId().equals(route.getId())));
            assertEquals(route.getId(), collectorService.getTodayRoute("COL001").orElseThrow().getId());
        } finally {
            routeDao.deleteById(route.getId());
        }
    }

    @Test
    @DisplayName("Service: Admin can create, update, and delete a zone")
    void testAdminZoneCrud() {
        AdminService adminService = new AdminService();
        String zoneId = "TEST_ZONE_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Zone zone = new Zone(zoneId, "Test Zone", "TZ", 12.9716, 77.5946, false);

        try {
            assertEquals(zone, adminService.saveZone(zone));
            Zone saved = adminService.getAllZones().stream()
                    .filter(item -> item.getId().equals(zoneId)).findFirst().orElseThrow();
            assertEquals("Test Zone", saved.getName());

            saved.setName("Updated Test Zone");
            assertTrue(adminService.updateZone(saved));
            assertEquals("Updated Test Zone", adminService.getAllZones().stream()
                    .filter(item -> item.getId().equals(zoneId)).findFirst().orElseThrow().getName());
        } finally {
            adminService.deleteZone(zoneId);
        }
        assertTrue(adminService.getAllZones().stream().noneMatch(item -> item.getId().equals(zoneId)));
    }

    @Test
    @DisplayName("Service: Admin can create, update, and delete a vehicle")
    void testAdminVehicleCrud() {
        AdminService adminService = new AdminService();
        VehicleDao vehicleDao = new VehicleDaoImpl();
        String vehicleId = "TEST_VEH_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        String registration = "TEST-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Vehicle vehicle = new SmallVehicle(vehicleId, registration, 900.0);

        try {
            assertEquals(vehicle, adminService.saveVehicle(vehicle));
            Vehicle saved = vehicleDao.findById(vehicleId).orElseThrow();
            assertEquals(registration, saved.getRegistrationNumber());

            saved.setRegistrationNumber(registration + "-UPDATED");
            saved.setCapacityKg(1200.0);
            assertTrue(adminService.updateVehicle(saved));
            Vehicle updated = vehicleDao.findById(vehicleId).orElseThrow();
            assertEquals(registration + "-UPDATED", updated.getRegistrationNumber());
            assertEquals(1200.0, updated.getCapacityKg());
        } finally {
            adminService.deleteVehicle(vehicleId);
        }
        assertTrue(vehicleDao.findById(vehicleId).isEmpty());
    }

    @Test
    @DisplayName("Service: Admin can create, update, and delete a household with coordinates")
    void testAdminHouseholdCrud() {
        AdminService adminService = new AdminService();
        HouseholdDao householdDao = new HouseholdDaoImpl();
        String householdId = "TEST_HH_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Household household = new Household(householdId, "Z1", null, "10 Test Lane",
                new Location(12.9716, 77.5946, "10 Test Lane"));

        try {
            assertEquals(household, adminService.saveHousehold(household));
            Household saved = householdDao.findById(householdId).orElseThrow();
            assertEquals("10 Test Lane", saved.getAddress());
            assertEquals(12.9716, saved.getLocation().getLatitude(), 0.0001);

            saved.setAddress("11 Test Lane");
            saved.setLocation(new Location(12.9720, 77.5950, "11 Test Lane"));
            assertTrue(adminService.updateHousehold(saved));
            Household updated = householdDao.findById(householdId).orElseThrow();
            assertEquals("11 Test Lane", updated.getAddress());
            assertEquals(12.9720, updated.getLocation().getLatitude(), 0.0001);
        } finally {
            adminService.deleteHousehold(householdId);
        }
        assertTrue(householdDao.findById(householdId).isEmpty());
    }

    @Test
    @DisplayName("Service: Admin bin CRUD and filtered pagination persist data")
    void testAdminBinCrudAndPaging() {
        AdminService adminService = new AdminService();
        BinDao binDao = new BinDaoImpl();
        String binId = "TEST_BIN_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        String binCode = "WC-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Bin bin = new Bin(binId, binCode, "Z1", new Location(12.9716, 77.5946, "Test bin"),
                WasteType.DRY, 240.0, 22.0, LocalDateTime.now(), BinStatus.NORMAL, 0);

        try {
            assertEquals(bin, adminService.saveBin(bin));
            assertTrue(adminService.getBinsCount("Z1", "DRY", binCode) >= 1);
            assertTrue(adminService.getBinsPage(0, 10, "Z1", "DRY", binCode).stream()
                    .anyMatch(saved -> saved.getId().equals(binId)));

            bin.setCapacityLiters(360.0);
            bin.setFillLevel(48.0);
            assertTrue(adminService.updateBin(bin));
            Bin updated = binDao.findById(binId).orElseThrow();
            assertEquals(360.0, updated.getCapacityLiters());
            assertEquals(48.0, updated.getCurrentFillPercentage());
        } finally {
            adminService.deleteBin(binId);
        }
        assertTrue(binDao.findById(binId).isEmpty());
    }

    @Test
    @DisplayName("Service: Admin creates and manages an authenticated collector account")
    void testAdminCollectorAccountCrud() {
        AdminService adminService = new AdminService();
        UserDao userDao = new UserDaoImpl();
        String email = "collector." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@wasteconnect.com";
        String employeeId = "EMP-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Collector collector = adminService.saveCollector("Test Collector", email, "TempPass123",
                employeeId, "Z1", "V1");

        try {
            assertTrue(com.wasteconnect.util.PasswordHasher.verifyPassword("TempPass123", collector.getPasswordHash()));
            assertTrue(userDao.findByEmail(email).isPresent());
            collector.setAssignedZoneId("Z2");
            collector.setAssignedVehicleId("V2");
            collector.setActive(false);
            assertTrue(adminService.updateCollector(collector));
            Collector updated = adminService.getAllCollectors().stream()
                    .filter(item -> item.getId().equals(collector.getId())).findFirst().orElseThrow();
            assertEquals("Z2", updated.getAssignedZoneId());
            assertEquals("V2", updated.getAssignedVehicleId());
            assertFalse(updated.isActive());
        } finally {
            adminService.deleteCollector(collector.getId());
        }
        assertTrue(userDao.findByEmail(email).isEmpty());
    }

    @Test
    @DisplayName("Service: Admin assignment stores the selected zone collector")
    void testAdminAssignsComplaintToZoneCollector() {
        AdminService adminService = new AdminService();
        ComplaintDao complaintDao = new ComplaintDaoImpl();
        String complaintId = "CMP-ASSIGN-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        complaintDao.save(new Complaint(complaintId, "CIT001", ComplaintType.OTHER,
                "Test assignment", new Location(12.9716, 77.5946, "Test"), "Z1"));

        try {
            String collectorId = adminService.assignComplaintToZoneCollector(complaintId);
            Complaint assigned = complaintDao.findById(complaintId).orElseThrow();
            assertNotNull(collectorId);
            assertEquals(collectorId, assigned.getAssignedCollectorId());
        } finally {
            complaintDao.deleteById(complaintId);
        }
    }

    @Test
    @DisplayName("Service: Admin complaint transitions update status and timeline")
    void testAdminComplaintStatusTransitions() {
        AdminService adminService = new AdminService();
        ComplaintDao complaintDao = new ComplaintDaoImpl();
        String complaintId = "CMP-STATUS-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        complaintDao.save(new Complaint(complaintId, "CIT001", ComplaintType.OTHER,
                "Test status transition", new Location(12.9716, 77.5946, "Test"), "Z1"));

        try {
            assertTrue(adminService.updateComplaintStatus(complaintId, ComplaintStatus.IN_PROGRESS, "Work started"));
            assertTrue(adminService.updateComplaintStatus(complaintId, ComplaintStatus.RESOLVED, "Issue fixed"));
            Complaint resolved = complaintDao.findById(complaintId).orElseThrow();
            assertEquals(ComplaintStatus.RESOLVED, resolved.getStatus());
            List<ComplaintEvent> timeline = complaintDao.findEventsForComplaint(complaintId);
            assertEquals(2, timeline.size());
            assertEquals(ComplaintStatus.IN_PROGRESS, timeline.get(0).getToStatus());
            assertEquals(ComplaintStatus.RESOLVED, timeline.get(1).getToStatus());
            assertThrows(ValidationException.class,
                    () -> adminService.updateComplaintStatus(complaintId, ComplaintStatus.ESCALATED, "Try after close"));
        } finally {
            complaintDao.deleteById(complaintId);
        }
    }

    @Test
    @DisplayName("Service: Admin can change and restore waste picker verification")
    void testAdminPickerVerification() {
        AdminService adminService = new AdminService();
        WastePickerDao pickerDao = new WastePickerDaoImpl();
        WastePicker picker = pickerDao.findById("WPK001").orElseThrow();
        boolean originalStatus = picker.isVerified();

        try {
            assertTrue(adminService.verifyWastePicker(picker.getId(), !originalStatus));
            assertEquals(!originalStatus, pickerDao.findById(picker.getId()).orElseThrow().isVerified());
        } finally {
            adminService.verifyWastePicker(picker.getId(), originalStatus);
        }
        assertEquals(originalStatus, pickerDao.findById(picker.getId()).orElseThrow().isVerified());
    }

    @Test
    @DisplayName("Service: HotspotDetector ranks vulnerable zones with recommendations")
    void testHotspotDetection() {
        AdminService adminService = new AdminService();
        List<HotspotReport> hotspots = adminService.detectHotspots();

        assertNotNull(hotspots);
        assertFalse(hotspots.isEmpty());
        // Verify descending order of risk score
        for (int i = 0; i < hotspots.size() - 1; i++) {
            assertTrue(hotspots.get(i).riskScore() >= hotspots.get(i + 1).riskScore(),
                    "Hotspots must be sorted descending by risk score");
        }
        assertNotNull(hotspots.get(0).recommendation());
    }

    @Test
    @DisplayName("DAO: VehicleDao queries active fleet")
    void testVehicleDao() {
        VehicleDao vehicleDao = new VehicleDaoImpl();
        List<Vehicle> activeVehicles = vehicleDao.findActive();
        assertFalse(activeVehicles.isEmpty());
        assertTrue(activeVehicles.stream().allMatch(Vehicle::isActive));
    }

    @Test
    @DisplayName("DAO: ZoneDao queries by code and center coordinates")
    void testZoneDao() {
        ZoneDao zoneDao = new ZoneDaoImpl();
        Optional<Zone> z1 = zoneDao.findByCode("CCH");
        assertTrue(z1.isPresent());
        assertEquals("Central Commercial Hub", z1.get().getName());
        assertTrue(z1.get().isMarketArea());
    }

    @Test
    @DisplayName("Service: WastePicker claims listing and completes pickup with earnings")
    void testWastePickerWorkflows() {
        com.wasteconnect.service.WastePickerService pickerService = new com.wasteconnect.service.WastePickerService();
        RecyclableListingDao listingDao = new RecyclableListingDaoImpl();
        WastePickerDao pickerDao = new WastePickerDaoImpl();
        CitizenDao citizenDao = new CitizenDaoImpl();
        RewardTransactionDao rewardDao = new RewardTransactionDaoImpl();
        Citizen citizenBefore = citizenDao.findById("CIT001").orElseThrow();
        WastePicker pickerBefore = pickerDao.findById("WPK001").orElseThrow();
        java.util.Set<String> transactionIdsBefore = rewardDao.findByCitizen("CIT001").stream()
            .map(RewardTransaction::getId).collect(java.util.stream.Collectors.toSet());

        RecyclableListing listing = new RecyclableListing("LST_TEST_PICK", "CIT001", WasteType.DRY,
                20.0, "10 Main Rd", new Location(12.9716, 77.5946, "Site"), 240.0);
        listingDao.save(listing);

        try {
            assertTrue(pickerService.claimListing("LST_TEST_PICK", "WPK001"));

            assertThrows(ValidationException.class,
                () -> pickerService.completeCollection("LST_TEST_PICK", 22.0, "WPK002"),
                "A different picker must not complete a listing claimed by WPK001");

            assertTrue(pickerService.completeCollection("LST_TEST_PICK", 22.0, "WPK001"));

            RecyclableListing finished = listingDao.findById("LST_TEST_PICK").orElseThrow();
            assertEquals(ListingStatus.COLLECTED, finished.getStatus());
            assertEquals(citizenBefore.getPointsBalance() + 220,
                citizenDao.findById("CIT001").orElseThrow().getPointsBalance());
            assertTrue(rewardDao.findByCitizen("CIT001").stream()
                .anyMatch(tx -> !transactionIdsBefore.contains(tx.getId())
                    && tx.getDescription().contains("LST_TEST_PICK") && tx.getPoints() == 220));
            assertThrows(ValidationException.class,
                () -> pickerService.completeCollection("LST_TEST_PICK", 22.0, "WPK001"),
                "A completed listing cannot be paid twice");
        } finally {
            listingDao.deleteById("LST_TEST_PICK");
            citizenDao.updatePointsOnly("CIT001", citizenBefore.getPointsBalance());
            rewardDao.findByCitizen("CIT001").stream()
                .filter(tx -> !transactionIdsBefore.contains(tx.getId()))
                .forEach(tx -> rewardDao.deleteById(tx.getId()));
            pickerDao.update(pickerBefore);
        }

    }

    @Test
    @DisplayName("Service: Segregation guide contains at least 40 categorized items")
    void testSegregationGuideItemCount() {
        List<CitizenService.SegregationGuideItem> guide = CitizenService.getSegregationGuide();
        assertNotNull(guide);
        assertTrue(guide.size() >= 40, "Segregation guide must contain at least 40 items. Actual: " + guide.size());
    }

    @Test
    @DisplayName("Service: AdminService calculates valid macro municipal analytics")
    void testMunicipalAnalyticsCalculation() {
        AdminService adminService = new AdminService();
        RouteDao routeDao = new RouteDaoImpl();
        SegregationLogDao segregationLogDao = new SegregationLogDaoImpl();
        ComplaintDao complaintDao = new ComplaintDaoImpl();
        WasteAnalytics analytics = adminService.getMunicipalAnalytics();
        assertNotNull(analytics);
        List<Route> routes = routeDao.findAll();
        long plannedStops = routes.stream().mapToLong(Route::getStopsCount).sum();
        long completedStops = routes.stream().mapToLong(Route::getCompletedStopsCount).sum();
        double expectedEfficiency = plannedStops == 0 ? 0.0 : Math.round((double) completedStops / plannedStops * 1000.0) / 10.0;
        List<SegregationLog> logs = segregationLogDao.findRecent(1000);
        long goodLogs = logs.stream().filter(log -> log.getQuality() == SegregationQuality.GOOD).count();
        double expectedSegregation = logs.isEmpty() ? 0.0 : Math.round((double) goodLogs / logs.size() * 1000.0) / 10.0;
        List<Complaint> complaints = complaintDao.findAll();
        long resolvedComplaints = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();
        double expectedResolution = complaints.isEmpty() ? 0.0 : Math.round((double) resolvedComplaints / complaints.size() * 1000.0) / 10.0;
        assertEquals(expectedEfficiency, analytics.collectionEfficiencyPct());
        assertEquals(expectedSegregation, analytics.averageSegregationRatePct());
        assertEquals(expectedResolution, analytics.complaintsResolvedPct());
    }
}
