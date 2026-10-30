package com.wasteconnect.test;

import com.wasteconnect.dao.DatabaseConnection;
import com.wasteconnect.exception.AuthenticationException;
import com.wasteconnect.exception.AuthorizationException;
import com.wasteconnect.exception.DuplicateEntryException;
import com.wasteconnect.exception.ValidationException;
import com.wasteconnect.model.*;
import com.wasteconnect.pattern.PriorityStrategy;
import com.wasteconnect.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Integration Tests T1 - T8 covering complete municipal operations lifecycle.
 */
public class EndToEndFlowsTest {

    private final AuthService authService = new AuthService();
    private final AdminService adminService = new AdminService();
    private final CitizenService citizenService = new CitizenService();
    private final CollectorService collectorService = new CollectorService();
    private final WastePickerService pickerService = new WastePickerService();
    private final NotificationService notificationService = new NotificationService();

    @BeforeAll
    static void initDb() {
        DatabaseConnection.getInstance();
    }

    @AfterEach
    void resetSession() {
        AuthService.logout();
    }

    @Test
    @DisplayName("T1: User Creation, Role Login & Duplicate Email Handling")
    void testT1_UserCreationAndAuthentication() {
        // Log in as Admin to obtain authorization
        User admin = authService.login("admin@wasteconnect.com", "Admin@123", UserRole.ADMIN);
        assertNotNull(admin);
        assertEquals(UserRole.ADMIN, admin.getRole());

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String citEmail = "citizen_" + uniqueSuffix + "@wasteconnect.com";
        String pickEmail = "picker_" + uniqueSuffix + "@wasteconnect.com";
        String colEmail = "collector_" + uniqueSuffix + "@wasteconnect.com";

        // Admin registers a new Citizen
        Citizen newCit = authService.registerCitizen("Resident " + uniqueSuffix, citEmail, "Citizen@123",
                "104 Green Avenue", "+91-9876543210", "Z1");
        assertNotNull(newCit);
        assertEquals(citEmail, newCit.getEmail());

        // Admin registers a new Waste Picker
        WastePicker newPick = authService.registerWastePicker("Picker " + uniqueSuffix, pickEmail, "Picker@123",
                "+91-9123456789", "Electric Cargo Cart");
        assertNotNull(newPick);
        assertEquals(pickEmail, newPick.getEmail());

        // Admin registers a new Collector
        Collector newCol = adminService.saveCollector("Collector " + uniqueSuffix, colEmail, "Collector@123",
                "EMP-" + uniqueSuffix, "Z1", "V1");
        assertNotNull(newCol);
        assertEquals(colEmail, newCol.getEmail());

        // Log out admin
        AuthService.logout();

        // Verify Citizen can log in with credentials
        User loggedCit = authService.login(citEmail, "Citizen@123", UserRole.CITIZEN);
        assertNotNull(loggedCit);
        assertEquals(UserRole.CITIZEN, loggedCit.getRole());
        AuthService.logout();

        // Verify Waste Picker can log in with credentials
        User loggedPick = authService.login(pickEmail, "Picker@123", UserRole.WASTE_PICKER);
        assertNotNull(loggedPick);
        assertEquals(UserRole.WASTE_PICKER, loggedPick.getRole());
        AuthService.logout();

        // Verify Collector can log in with credentials
        User loggedCol = authService.login(colEmail, "Collector@123", UserRole.COLLECTOR);
        assertNotNull(loggedCol);
        assertEquals(UserRole.COLLECTOR, loggedCol.getRole());
        AuthService.logout();

        // Verify duplicate email registration is rejected
        authService.login("admin@wasteconnect.com", "Admin@123");
        assertThrows(DuplicateEntryException.class, () ->
                authService.registerCitizen("Duplicate Resident", citEmail, "Citizen@123", "104 Green Ave", "+91-9876543210", "Z1")
        );
    }

    @Test
    @DisplayName("T2: Smart Route Generation and Collector Assignment")
    void testT2_RouteGenerationAndCollectorAssignment() {
        authService.login("admin@wasteconnect.com", "Admin@123");

        Zone zone = adminService.getAllZones().get(0);
        Vehicle vehicle = adminService.getAllVehicles().get(0);
        Collector collector = adminService.getAllCollectors().get(0);

        Route route = adminService.planRoute(zone.getId(), vehicle.getId(), collector.getId(), new PriorityStrategy());
        assertNotNull(route);
        assertEquals(collector.getId(), route.getCollectorId());
        assertFalse(route.getStops().isEmpty(), "Route should contain sequenced collection stops");

        // Verify collector can view the generated route for today
        Optional<Route> collectorRoute = collectorService.getTodayRoute(collector.getId());
        assertTrue(collectorRoute.isPresent());
        assertEquals(route.getId(), collectorRoute.get().getId());
    }

    @Test
    @DisplayName("T3: Stop Collection, Segregation Quality & Citizen Reward Points")
    void testT3_StopCollectionAndPointsAward() {
        // Seed a citizen
        authService.login("admin@wasteconnect.com", "Admin@123");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Citizen citizen = authService.registerCitizen("Resident T3 " + suffix, "res_t3_" + suffix + "@wasteconnect.com",
                "Password@123", "202 Clean Street", "+91-9888877777", "Z1");

        Collector collector = adminService.getAllCollectors().get(0);
        Route route = adminService.planRoute("Z1", "V1", collector.getId(), new PriorityStrategy());
        RouteStop stop = route.getStops().get(0);

        // Record stop collection with GOOD segregation
        collectorService.recordStopCollected(route.getId(), stop.getSequence(), stop.getStopId(),
                SegregationQuality.GOOD, 15.0, citizen.getId());

        // Verify citizen points increased
        Citizen updated = citizenService.getCitizen(citizen.getId()).orElseThrow();
        assertTrue(updated.getPointsBalance() > 0, "Citizen points balance should increase for good segregation");

        // Verify points ledger transaction recorded
        List<RewardTransaction> transactions = citizenService.getRewardTransactions(citizen.getId());
        assertFalse(transactions.isEmpty());
        assertEquals(RewardTransaction.Type.EARNED, transactions.get(0).getType());

        // Verify citizen received notification
        List<Notification> notifications = notificationService.getUserNotifications(citizen.getId());
        assertFalse(notifications.isEmpty());
    }

    @Test
    @DisplayName("T4: Citizen Complaint Flow with Auto-Assignment and Resolution")
    void testT4_ComplaintFlow() {
        authService.login("admin@wasteconnect.com", "Admin@123");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Citizen citizen = authService.registerCitizen("Resident T4 " + suffix, "res_t4_" + suffix + "@wasteconnect.com",
                "Password@123", "303 Waste Lane", "+91-9888866666", "Z1");

        // Citizen raises complaint
        Complaint complaint = citizenService.raiseComplaint(citizen.getId(), ComplaintType.OVERFLOWING_BIN,
                "Bin overflowing on 3rd corner", 12.9720, 77.5950, "Z1");
        assertNotNull(complaint);
        assertEquals(ComplaintStatus.OPEN, complaint.getStatus());
        assertNotNull(complaint.getAssignedCollectorId(), "Complaint should be auto-assigned to zone collector");

        // Collector resolves complaint
        String collectorId = complaint.getAssignedCollectorId();
        boolean resolved = collectorService.resolveComplaint(complaint.getId(), "Bin emptied and area cleaned.");
        assertTrue(resolved);

        // Verify complaint status resolved in citizen view
        List<Complaint> citizenComplaints = citizenService.getCitizenComplaints(citizen.getId());
        Complaint updatedComplaint = citizenComplaints.stream()
                .filter(c -> c.getId().equals(complaint.getId())).findFirst().orElseThrow();
        assertEquals(ComplaintStatus.RESOLVED, updatedComplaint.getStatus());

        // Verify citizen received resolution notification
        List<Notification> notifications = notificationService.getUserNotifications(citizen.getId());
        assertTrue(notifications.stream().anyMatch(n -> n.getTitle().contains("Complaint Resolved")));
    }

    @Test
    @DisplayName("T5: Recyclable Handoff Lifecycle (Citizen -> Picker -> Earnings)")
    void testT5_RecyclableListingLifecycle() {
        authService.login("admin@wasteconnect.com", "Admin@123");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Citizen citizen = authService.registerCitizen("Resident T5 " + suffix, "res_t5_" + suffix + "@wasteconnect.com",
                "Password@123", "404 Recycle St", "+91-9888855555", "Z1");

        WastePicker picker = authService.registerWastePicker("Picker T5 " + suffix, "pick_t5_" + suffix + "@wasteconnect.com",
                "Password@123", "+91-9888844444", "Cargo Van");
        // Verify picker
        adminService.verifyWastePicker(picker.getId(), true);

        // Citizen offers recyclables
        RecyclableListing listing = citizenService.listRecyclables(citizen.getId(), WasteType.DRY, 25.0,
                "404 Recycle St", 12.9730, 77.5960);
        assertNotNull(listing);
        assertEquals(ListingStatus.AVAILABLE, listing.getStatus());

        // Picker claims listing
        boolean claimed = pickerService.claimListing(listing.getId(), picker.getId());
        assertTrue(claimed);

        // Picker completes collection
        boolean completed = pickerService.completeCollection(listing.getId(), 25.0, picker.getId());
        assertTrue(completed);

        // Verify picker earnings increased
        WastePicker updatedPicker = pickerService.getPicker(picker.getId()).orElseThrow();
        assertTrue(updatedPicker.getTotalEarnings() > 0.0);
        assertEquals(25.0, updatedPicker.getTotalKgCollected());

        // Verify citizen received points for recyclables
        Citizen updatedCitizen = citizenService.getCitizen(citizen.getId()).orElseThrow();
        assertTrue(updatedCitizen.getPointsBalance() > 0);
    }

    @Test
    @DisplayName("T6: Collector Road Obstruction & Dynamic Route Replanning")
    void testT6_RoadObstructionAndReplanning() {
        authService.login("admin@wasteconnect.com", "Admin@123");
        Collector collector = adminService.getAllCollectors().get(0);
        Route route = adminService.planRoute("Z1", "V1", collector.getId(), new PriorityStrategy());
        assertTrue(route.getStops().size() > 1, "Route must have at least 2 stops for replanning test");

        RouteStop firstStop = route.getStops().get(0);
        Route replanned = collectorService.reportBlockedRoad(route.getId(), firstStop.getSequence(), "Tree branch blocking road");
        assertNotNull(replanned);

        // Verify route stops were updated
        Route updatedRoute = collectorService.getTodayRoute(collector.getId()).orElseThrow();
        assertNotNull(updatedRoute);
    }

    @Test
    @DisplayName("T7: Point Redemption & Insufficient Balance Validation")
    void testT7_PointRedemptionValidation() {
        authService.login("admin@wasteconnect.com", "Admin@123");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Citizen citizen = authService.registerCitizen("Resident T7 " + suffix, "res_t7_" + suffix + "@wasteconnect.com",
                "Password@123", "505 Reward Blvd", "+91-9888833333", "Z1");

        // Attempting to redeem more points than available (new citizen has 50 welcome points) throws ValidationException
        assertThrows(ValidationException.class, () ->
                citizenService.redeemPoints(citizen.getId(), 500, "Municipal Bus Pass")
        );

        // Award points via segregation collection
        Collector collector = adminService.getAllCollectors().get(0);
        Route route = adminService.planRoute("Z1", "V1", collector.getId(), new PriorityStrategy());
        collectorService.recordStopCollected(route.getId(), route.getStops().get(0).getSequence(),
                route.getStops().get(0).getStopId(), SegregationQuality.GOOD, 20.0, citizen.getId());

        Citizen citizenWithPoints = citizenService.getCitizen(citizen.getId()).orElseThrow();
        int initialBalance = citizenWithPoints.getPointsBalance();
        assertTrue(initialBalance >= 20);

        // Now redeem within balance
        boolean success = citizenService.redeemPoints(citizen.getId(), 10, "Grocery Coupon");
        assertTrue(success);

        Citizen afterRedeem = citizenService.getCitizen(citizen.getId()).orElseThrow();
        assertEquals(initialBalance - 10, afterRedeem.getPointsBalance());
    }

    @Test
    @DisplayName("T8: Admin-Only Registration Security Guard")
    void testT8_AdminOnlyRegistrationGuard() {
        // 1. Without any login (anonymous), user creation throws AuthorizationException
        AuthService.logout();
        assertThrows(AuthorizationException.class, () ->
                authService.registerCitizen("Unauthorized", "anon@wasteconnect.com", "Pass@123", "Addr", "9999999999", "Z1")
        );
        assertThrows(AuthorizationException.class, () ->
                authService.registerWastePicker("Unauthorized", "anon_pick@wasteconnect.com", "Pass@123", "9999999999", "Cart")
        );

        // 2. When logged in as non-admin (Citizen), user creation also throws AuthorizationException
        authService.login("admin@wasteconnect.com", "Admin@123");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Citizen citizen = authService.registerCitizen("Citizen Guard " + suffix, "guard_" + suffix + "@wasteconnect.com",
                "Password@123", "707 Guard Way", "+91-9888822222", "Z1");

        // Log in as the Citizen
        AuthService.logout();
        authService.login(citizen.getEmail(), "Password@123", UserRole.CITIZEN);

        assertThrows(AuthorizationException.class, () ->
                authService.registerCitizen("Should Fail", "fail_" + suffix + "@wasteconnect.com", "Pass@123", "Addr", "9999999999", "Z1")
        );
        assertThrows(AuthorizationException.class, () ->
                authService.registerWastePicker("Should Fail", "fail_p_" + suffix + "@wasteconnect.com", "Pass@123", "9999999999", "Cart")
        );
    }
}
