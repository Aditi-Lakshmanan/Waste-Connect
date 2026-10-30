package com.wasteconnect.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * INHERITANCE: Admin represents municipal supervisory staff possessing elevated administrative privileges.
 */
public class Admin extends User {
    private String department;
    private int accessLevel; // 1 = Supervisor, 2 = Manager, 3 = SuperAdmin

    public Admin(String id, String name, String email, String passwordHash, String department, int accessLevel) {
        super(id, name, email, passwordHash, UserRole.ADMIN);
        this.department = department;
        this.accessLevel = Math.max(1, accessLevel);
    }

    public Admin(String id, String name, String email, String passwordHash, LocalDateTime createdAt, boolean active,
                 String department, int accessLevel) {
        super(id, name, email, passwordHash, UserRole.ADMIN, createdAt, active);
        this.department = department;
        this.accessLevel = Math.max(1, accessLevel);
    }

    // POLYMORPHISM: Admin dashboard view
    @Override
    public String getDashboardView() {
        return "ADMIN_DASHBOARD";
    }

    // POLYMORPHISM: Full administrative permission scope
    @Override
    public Set<String> getPermissions() {
        return Collections.unmodifiableSet(new HashSet<>(Set.of(
                "MANAGE_ZONES", "MANAGE_BINS", "MANAGE_VEHICLES", "MANAGE_COLLECTORS",
                "RUN_ROUTING", "MANAGE_COMPLAINTS", "VIEW_ANALYTICS", "VERIFY_PICKERS",
                "RUN_SIMULATOR", "EXPORT_REPORTS", "BENCHMARK_ROUTING"
        )));
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(int accessLevel) {
        this.accessLevel = Math.max(1, accessLevel);
    }
}
