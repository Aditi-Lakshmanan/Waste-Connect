package com.wasteconnect.pattern;

import com.wasteconnect.model.*;

import java.util.UUID;

/**
 * CO2 & DESIGN PATTERN: Factory pattern for generating concrete User domain actors.
 */
public final class UserFactory {

    private UserFactory() {}

    public static Citizen createCitizen(String name, String email, String passwordHash, String address, String phone, String zoneId) {
        String id = "CIT-" + UUID.randomUUID().toString().substring(0, 8);
        return new Citizen(id, name, email, passwordHash, address, phone, zoneId);
    }

    public static Collector createCollector(String name, String email, String passwordHash, String employeeId, String zoneId, String vehicleId) {
        String id = "COL-" + UUID.randomUUID().toString().substring(0, 8);
        return new Collector(id, name, email, passwordHash, employeeId, zoneId, vehicleId);
    }

    public static Admin createAdmin(String name, String email, String passwordHash, String department, int accessLevel) {
        String id = "ADM-" + UUID.randomUUID().toString().substring(0, 8);
        return new Admin(id, name, email, passwordHash, department, accessLevel);
    }

    public static WastePicker createWastePicker(String name, String email, String passwordHash, String phone, String vehicleDesc) {
        String id = "WPK-" + UUID.randomUUID().toString().substring(0, 8);
        return new WastePicker(id, name, email, passwordHash, phone, vehicleDesc);
    }
}
