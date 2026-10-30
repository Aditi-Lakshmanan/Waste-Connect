package com.wasteconnect.test;

import com.wasteconnect.exception.BinOverflowException;
import com.wasteconnect.model.*;
import com.wasteconnect.pattern.WasteFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite verifying the Four Pillars of Object-Oriented Programming.
 */
public class OopPillarsTest {

    @Test
    @DisplayName("Polymorphism: Waste subtypes calculate reward points differently")
    void testWastePolymorphicRewardPoints() {
        Waste wet = WasteFactory.createWaste(WasteType.WET, 10.0);
        Waste dry = WasteFactory.createWaste(WasteType.DRY, 10.0);
        Waste hazardous = WasteFactory.createWaste(WasteType.HAZARDOUS, 10.0);
        Waste eWaste = WasteFactory.createWaste(WasteType.E_WASTE, 10.0);

        // POLYMORPHISM verification
        assertEquals(50, wet.calculateRewardPoints(10.0), "Wet waste should yield 5 pts/kg");
        assertEquals(100, dry.calculateRewardPoints(10.0), "Dry waste should yield 10 pts/kg");
        assertEquals(2, hazardous.calculateRewardPoints(10.0), "Hazardous waste gives fixed token of 2 pts");
        assertEquals(200, eWaste.calculateRewardPoints(10.0), "E-waste should yield 20 pts/kg");
    }

    @Test
    @DisplayName("Polymorphism: Waste subtypes provide distinct handling instructions")
    void testWastePolymorphicHandlingInstructions() {
        Waste wet = new WetWaste(5.0);
        Waste haz = new HazardousWaste(2.0);

        assertTrue(wet.getHandlingInstructions().contains("composting"), "Wet waste must mention composting");
        assertTrue(haz.getHandlingInstructions().contains("HAZARD"), "Hazardous waste must contain warning");
    }

    @Test
    @DisplayName("Polymorphism: Users return role-specific dashboard views")
    void testUserPolymorphicDashboardViews() {
        User citizen = new Citizen("C1", "Alice", "alice@test.com", "hash", "Addr", "999", "Z1");
        User collector = new Collector("CL1", "Bob", "bob@test.com", "hash", "EMP1", "Z1", "V1");
        User admin = new Admin("A1", "Charlie", "charlie@test.com", "hash", "Sanitation", 2);
        User picker = new WastePicker("P1", "David", "david@test.com", "hash", "888", "Van");

        assertEquals("CITIZEN_DASHBOARD", citizen.getDashboardView());
        assertEquals("COLLECTOR_DASHBOARD", collector.getDashboardView());
        assertEquals("ADMIN_DASHBOARD", admin.getDashboardView());
        assertEquals("PICKER_DASHBOARD", picker.getDashboardView());
    }

    @Test
    @DisplayName("Encapsulation: Bin rejects fill levels outside 0.0 - 100.0%")
    void testBinFillLevelEncapsulation() {
        Location loc = new Location(12.97, 77.59, "MG Road");
        Bin bin = new Bin("B1", "BIN-001", "Z1", loc, WasteType.DRY, 240.0);

        // Valid updates
        bin.setFillLevel(50.0);
        assertEquals(50.0, bin.getCurrentFillPercentage());
        assertEquals(BinStatus.NORMAL, bin.getStatus());

        bin.setFillLevel(85.0);
        assertEquals(BinStatus.WARNING, bin.getStatus());

        // Invalid negative value must throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> bin.setFillLevel(-5.0));

        // Invalid value over 100 must throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> bin.setFillLevel(105.0));

        // At 100% overflow exception must trigger
        assertThrows(BinOverflowException.class, () -> bin.setFillLevel(100.0));
    }

    @Test
    @DisplayName("Encapsulation: Citizen rejects negative reward points and overdrafts")
    void testCitizenRewardPointsEncapsulation() {
        Citizen citizen = new Citizen("C1", "Alice", "alice@test.com", "hash", "Addr", "999", "Z1");
        citizen.setPointsBalance(50);

        // Rejects negative points addition
        assertThrows(IllegalArgumentException.class, () -> citizen.addPoints(-10));

        // Rejects negative deduction
        assertThrows(IllegalArgumentException.class, () -> citizen.deductPoints(-5));

        // Rejects deduction greater than balance
        assertThrows(IllegalStateException.class, () -> citizen.deductPoints(100));

        citizen.addPoints(30);
        assertEquals(80, citizen.getPointsBalance());
        citizen.deductPoints(40);
        assertEquals(40, citizen.getPointsBalance());
    }

    @Test
    @DisplayName("Encapsulation: Location validates coordinate boundaries")
    void testLocationCoordinateEncapsulation() {
        assertThrows(IllegalArgumentException.class, () -> new Location(95.0, 77.0, "Invalid Lat"));
        assertThrows(IllegalArgumentException.class, () -> new Location(12.0, 190.0, "Invalid Lng"));

        Location valid = new Location(12.9716, 77.5946, "Bangalore Center");
        assertEquals(12.9716, valid.getLatitude());
        assertEquals(77.5946, valid.getLongitude());
    }

    @Test
    @DisplayName("Inheritance: Vehicles calculate fuel consumption and maneuverability per subtype")
    void testVehicleInheritanceAndPolymorphism() {
        Vehicle small = new SmallVehicle("V1", "KA-01-101", 1500.0);
        Vehicle truck = new LargeTruck("V2", "KA-01-201", 8000.0);

        assertTrue(small.canNavigateNarrowStreet(), "Small vehicle should navigate narrow streets");
        assertFalse(truck.canNavigateNarrowStreet(), "Large truck cannot navigate narrow alleyways");

        assertEquals(10.0, small.calculateFuelConsumption(120.0), 0.01, "Small vehicle uses 12 km/L");
        assertEquals(30.0, truck.calculateFuelConsumption(120.0), 0.01, "Large truck uses 4 km/L");
    }
}
