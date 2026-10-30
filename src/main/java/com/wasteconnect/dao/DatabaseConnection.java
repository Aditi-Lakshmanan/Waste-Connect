package com.wasteconnect.dao;

import com.wasteconnect.exception.DataAccessException;
import com.wasteconnect.util.AppLogger;
import com.wasteconnect.util.PasswordHasher;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * CO2 & DESIGN PATTERN: Singleton pattern providing thread-safe SQLite connection management,
 * schema initialization, foreign key enforcement, and database seeding.
 */
public final class DatabaseConnection {
    private static volatile DatabaseConnection instance;
    private static String dbUrl = "jdbc:h2:file:./data/wasteconnect;DB_CLOSE_DELAY=-1";
    private Connection connection;

    private DatabaseConnection() {
        initDb();
    }

    // SINGLETON PATTERN: Thread-safe double-checked locking
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Allows test harness to configure an in-memory or alternative SQLite instance.
     */
    public static synchronized void setDatabaseUrl(String url) {
        if (instance != null) {
            instance.close();
            instance = null;
        }
        dbUrl = url;
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(dbUrl);
                try (Statement stmt = connection.createStatement()) {
                    try {
                        stmt.execute("PRAGMA foreign_keys = ON;");
                    } catch (SQLException ignored) {
                        // H2 compatibility fallback for portable JDBC use
                    }
                    try {
                        stmt.execute("PRAGMA journal_mode = WAL;");
                    } catch (SQLException ignored) {
                        // SQLite-specific pragma is not supported by all JDBC backends
                    }
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to establish database connection: " + dbUrl, e);
        }
        return connection;
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            AppLogger.warning("Error closing database connection: " + e.getMessage());
        } finally {
            connection = null;
        }
    }

    private void initDb() {
        if (!dbUrl.contains(":memory:")) {
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
        }
        initializeSchema();
        seedInitialDataIfEmpty();
    }

    public void initializeSchema() {
        Connection conn = getConnection();
        try (Statement stmt = conn.createStatement()) {
            // Users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    active INTEGER NOT NULL DEFAULT 1
                );
            """);

            // Citizens table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS citizens (
                    id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                    streak INTEGER NOT NULL DEFAULT 0,
                    segregation_score REAL NOT NULL DEFAULT 100.0,
                    points_balance INTEGER NOT NULL DEFAULT 50,
                    address TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    zone_id TEXT,
                    household_id TEXT
                );
            """);

            // Collectors table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS collectors (
                    id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                    employee_id TEXT NOT NULL UNIQUE,
                    assigned_zone_id TEXT,
                    assigned_vehicle_id TEXT,
                    rating REAL NOT NULL DEFAULT 5.0,
                    total_pickups INTEGER NOT NULL DEFAULT 0
                );
            """);

            // Waste Pickers table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS waste_pickers (
                    id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                    phone TEXT NOT NULL,
                    vehicle_desc TEXT,
                    verified INTEGER NOT NULL DEFAULT 0,
                    total_earnings REAL NOT NULL DEFAULT 0.0,
                    total_kg_collected REAL NOT NULL DEFAULT 0.0
                );
            """);

            // Admins table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS admins (
                    id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                    department TEXT NOT NULL,
                    access_level INTEGER NOT NULL DEFAULT 1
                );
            """);

            // Zones table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS zones (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    code TEXT NOT NULL UNIQUE,
                    center_lat REAL NOT NULL,
                    center_lng REAL NOT NULL,
                    market_area INTEGER NOT NULL DEFAULT 0,
                    target_fill_limit REAL NOT NULL DEFAULT 80.0
                );
            """);

            // Households table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS households (
                    id TEXT PRIMARY KEY,
                    zone_id TEXT NOT NULL REFERENCES zones(id) ON DELETE CASCADE,
                    citizen_id TEXT,
                    address TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL
                );
            """);

            // Bins table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bins (
                    id TEXT PRIMARY KEY,
                    bin_code TEXT NOT NULL UNIQUE,
                    zone_id TEXT NOT NULL REFERENCES zones(id) ON DELETE CASCADE,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    waste_type TEXT NOT NULL,
                    capacity_liters REAL NOT NULL,
                    fill_percentage REAL NOT NULL,
                    last_collected_at TEXT NOT NULL,
                    status TEXT NOT NULL,
                    open_complaints INTEGER NOT NULL DEFAULT 0
                );
            """);

            // Vehicles table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS vehicles (
                    id TEXT PRIMARY KEY,
                    registration_number TEXT NOT NULL UNIQUE,
                    vehicle_type TEXT NOT NULL,
                    capacity_kg REAL NOT NULL,
                    active INTEGER NOT NULL DEFAULT 1
                );
            """);

            // Routes table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS routes (
                    id TEXT PRIMARY KEY,
                    zone_id TEXT NOT NULL,
                    collector_id TEXT,
                    vehicle_id TEXT,
                    route_date TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    status TEXT NOT NULL,
                    total_distance_km REAL NOT NULL,
                    estimated_hours REAL NOT NULL,
                    strategy_used TEXT NOT NULL
                );
            """);

            boolean hasRouteCreatedAt = false;
            try (ResultSet columns = conn.getMetaData().getColumns(null, null, null, null)) {
                while (columns.next()) {
                    if ("routes".equalsIgnoreCase(columns.getString("TABLE_NAME"))
                            && "created_at".equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                        hasRouteCreatedAt = true;
                        break;
                    }
                }
            }
            if (!hasRouteCreatedAt) {
                stmt.execute("ALTER TABLE routes ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;");
            }

            // Route stops table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS route_stops (
                    id TEXT PRIMARY KEY,
                    route_id TEXT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
                    sequence_num INTEGER NOT NULL,
                    stop_id TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    demand_kg REAL NOT NULL,
                    priority_score REAL NOT NULL,
                    collected INTEGER NOT NULL DEFAULT 0,
                    notes TEXT
                );
            """);

            // Complaints table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS complaints (
                    id TEXT PRIMARY KEY,
                    citizen_id TEXT NOT NULL,
                    complaint_type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    zone_id TEXT NOT NULL,
                    status TEXT NOT NULL,
                    assigned_collector_id TEXT,
                    created_at TEXT NOT NULL,
                    resolved_at TEXT,
                    resolution_notes TEXT
                );
            """);

            // Complaint events table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS complaint_events (
                    id TEXT PRIMARY KEY,
                    complaint_id TEXT NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
                    from_status TEXT,
                    to_status TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    remarks TEXT
                );
            """);

            // Pickups table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pickups (
                    id TEXT PRIMARY KEY,
                    citizen_id TEXT NOT NULL,
                    waste_type TEXT NOT NULL,
                    weight_kg REAL NOT NULL,
                    preferred_slot TEXT NOT NULL,
                    address TEXT NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    assigned_collector_id TEXT
                );
            """);

            // Segregation logs table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS segregation_logs (
                    id TEXT PRIMARY KEY,
                    citizen_id TEXT NOT NULL,
                    collector_id TEXT NOT NULL,
                    bin_id TEXT,
                    quality TEXT NOT NULL,
                    weight_kg REAL NOT NULL,
                    points_awarded INTEGER NOT NULL,
                    logged_at TEXT NOT NULL
                );
            """);

            // Reward transactions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reward_transactions (
                    id TEXT PRIMARY KEY,
                    citizen_id TEXT NOT NULL,
                    type TEXT NOT NULL,
                    points INTEGER NOT NULL,
                    description TEXT NOT NULL,
                    timestamp TEXT NOT NULL
                );
            """);

            // Recyclable listings table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS recyclable_listings (
                    id TEXT PRIMARY KEY,
                    citizen_id TEXT NOT NULL,
                    waste_type TEXT NOT NULL,
                    weight_kg REAL NOT NULL,
                    address TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    status TEXT NOT NULL,
                    claimed_picker_id TEXT,
                    created_at TEXT NOT NULL,
                    collected_at TEXT,
                    estimated_value REAL NOT NULL
                );
            """);

            // Notifications table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    message TEXT NOT NULL,
                    read INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL
                );
            """);

            // Settings table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    "key" TEXT PRIMARY KEY,
                    "value" TEXT NOT NULL,
                    description TEXT
                );
            """);

            // Database Indexes for Performance (CO5)
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bins_zone ON bins(zone_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bins_status ON bins(status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_households_zone ON households(zone_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_complaints_zone ON complaints(zone_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_complaints_status ON complaints(status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_routes_date ON routes(route_date);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_route_stops_route ON route_stops(route_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_segregation_citizen ON segregation_logs(citizen_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_rewards_citizen ON reward_transactions(citizen_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_listings_status ON recyclable_listings(status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, read);");

        } catch (SQLException e) {
            throw new DataAccessException("Failed to initialize database schema", e);
        }
    }

    public synchronized void seedInitialDataIfEmpty() {
        Connection conn = getConnection();
        try {
            try (Statement checkStmt = conn.createStatement();
                 ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM zones;")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return; // Already seeded
                }
            }

            conn.setAutoCommit(false); // CO3: Transactional batch insert
            AppLogger.info("Seeding initial database with 5 zones, 300+ households, 1,000+ bins, and default accounts...");

            // 1. Settings
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO settings (\"key\", \"value\", description) VALUES (?, ?, ?);")) {
                Object[][] defaultSettings = {
                        {"w1_fill_level", "0.45", "Weight for bin fill percentage in priority score"},
                        {"w2_days_since_pickup", "0.25", "Weight for days elapsed since last collection"},
                        {"w3_complaints", "0.15", "Weight for active open complaints around the bin"},
                        {"w4_waste_urgency", "0.15", "Weight for intrinsic waste urgency"},
                        {"simulation_speed_multiplier", "1.0", "Speed multiplier for IoT bin simulator"},
                        {"sla_hours_threshold", "2.0", "Hours before complaint auto-escalates"}
                };
                for (Object[] s : defaultSettings) {
                    pstmt.setString(1, (String) s[0]);
                    pstmt.setString(2, (String) s[1]);
                    pstmt.setString(3, (String) s[2]);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            // 2. Zones (5 municipal zones)
            String[][] zones = {
                    {"Z1", "Central Commercial Hub", "CCH", "12.9716", "77.5946", "1", "80.0"},
                    {"Z2", "Riverside Heritage Ward", "RHW", "12.9810", "77.6050", "0", "80.0"},
                    {"Z3", "Tech Park Cyber Corridor", "TPC", "12.9352", "77.6946", "0", "85.0"},
                    {"Z4", "Green Valley Suburbs", "GVS", "12.9141", "77.6329", "0", "75.0"},
                    {"Z5", "Metro Industrial Sector", "MIS", "12.9902", "77.5520", "1", "80.0"}
            };
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO zones VALUES (?, ?, ?, ?, ?, ?, ?);")) {
                for (String[] z : zones) {
                    pstmt.setString(1, z[0]);
                    pstmt.setString(2, z[1]);
                    pstmt.setString(3, z[2]);
                    pstmt.setDouble(4, Double.parseDouble(z[3]));
                    pstmt.setDouble(5, Double.parseDouble(z[4]));
                    pstmt.setInt(6, Integer.parseInt(z[5]));
                    pstmt.setDouble(7, Double.parseDouble(z[6]));
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            // 3. Vehicles (5 vehicles)
            String[][] vehicles = {
                    {"V1", "KA-01-WC-1001", "SMALL_VEHICLE", "1500.0", "1"},
                    {"V2", "KA-01-WC-1002", "SMALL_VEHICLE", "1500.0", "1"},
                    {"V3", "KA-01-WC-2001", "LARGE_TRUCK", "8000.0", "1"},
                    {"V4", "KA-01-WC-2002", "LARGE_TRUCK", "8000.0", "1"},
                    {"V5", "KA-01-WC-2003", "LARGE_TRUCK", "8000.0", "1"}
            };
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO vehicles VALUES (?, ?, ?, ?, ?);")) {
                for (String[] v : vehicles) {
                    pstmt.setString(1, v[0]);
                    pstmt.setString(2, v[1]);
                    pstmt.setString(3, v[2]);
                    pstmt.setDouble(4, Double.parseDouble(v[3]));
                    pstmt.setInt(5, Integer.parseInt(v[4]));
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            // 4. Default Accounts & Users
            // Default logins:
            // admin@wasteconnect.com / Admin@123
            // collector1@wasteconnect.com / Collect@123
            // citizen1@wasteconnect.com / Citizen@123
            // picker1@wasteconnect.com / Picker@123
            String adminHash = PasswordHasher.hashPassword("Admin@123");
            String collectorHash = PasswordHasher.hashPassword("Collect@123");
            String citizenHash = PasswordHasher.hashPassword("Citizen@123");
            String pickerHash = PasswordHasher.hashPassword("Picker@123");

            try (PreparedStatement pstmtUser = conn.prepareStatement("INSERT INTO users VALUES (?, ?, ?, ?, ?, ?, ?);");
                 PreparedStatement pstmtAdmin = conn.prepareStatement("INSERT INTO admins VALUES (?, ?, ?);");
                 PreparedStatement pstmtCollector = conn.prepareStatement("INSERT INTO collectors VALUES (?, ?, ?, ?, ?, ?);");
                 PreparedStatement pstmtCitizen = conn.prepareStatement("INSERT INTO citizens VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
                 PreparedStatement pstmtPicker = conn.prepareStatement("INSERT INTO waste_pickers VALUES (?, ?, ?, ?, ?, ?);")) {

                // Admin
                pstmtUser.setString(1, "ADM001");
                pstmtUser.setString(2, "Municipal Commissioner");
                pstmtUser.setString(3, "admin@wasteconnect.com");
                pstmtUser.setString(4, adminHash);
                pstmtUser.setString(5, "ADMIN");
                pstmtUser.setString(6, LocalDateTime.now().minusDays(90).toString());
                pstmtUser.setInt(7, 1);
                pstmtUser.addBatch();

                pstmtAdmin.setString(1, "ADM001");
                pstmtAdmin.setString(2, "Urban Sanitation Management");
                pstmtAdmin.setInt(3, 3);
                pstmtAdmin.addBatch();

                // 10 Collectors
                for (int i = 1; i <= 10; i++) {
                    String colId = "COL00" + i;
                    String email = "collector" + i + "@wasteconnect.com";
                    String name = "Collector Operative " + i;
                    String zoneId = "Z" + (((i - 1) % 5) + 1);
                    String vId = "V" + (((i - 1) % 5) + 1);

                    pstmtUser.setString(1, colId);
                    pstmtUser.setString(2, name);
                    pstmtUser.setString(3, email);
                    pstmtUser.setString(4, collectorHash);
                    pstmtUser.setString(5, "COLLECTOR");
                    pstmtUser.setString(6, LocalDateTime.now().minusDays(60).toString());
                    pstmtUser.setInt(7, 1);
                    pstmtUser.addBatch();

                    pstmtCollector.setString(1, colId);
                    pstmtCollector.setString(2, "EMP-" + (1000 + i));
                    pstmtCollector.setString(3, zoneId);
                    pstmtCollector.setString(4, vId);
                    pstmtCollector.setDouble(5, 4.8);
                    pstmtCollector.setInt(6, 150 + i * 12);
                    pstmtCollector.addBatch();
                }

                // 10 Waste Pickers
                for (int i = 1; i <= 10; i++) {
                    String picId = "WPK00" + i;
                    String email = "picker" + i + "@wasteconnect.com";
                    String name = "Eco Collector " + i;

                    pstmtUser.setString(1, picId);
                    pstmtUser.setString(2, name);
                    pstmtUser.setString(3, email);
                    pstmtUser.setString(4, pickerHash);
                    pstmtUser.setString(5, "WASTE_PICKER");
                    pstmtUser.setString(6, LocalDateTime.now().minusDays(45).toString());
                    pstmtUser.setInt(7, 1);
                    pstmtUser.addBatch();

                    pstmtPicker.setString(1, picId);
                    pstmtPicker.setString(2, "+91-98765" + String.format("%05d", 1000 + i));
                    pstmtPicker.setString(3, i % 2 == 0 ? "Cargo Tricycle" : "Small EV Van");
                    pstmtPicker.setInt(4, 1); // all verified
                    pstmtPicker.setDouble(5, 4500.0 + i * 400);
                    pstmtPicker.setDouble(6, 600.0 + i * 50);
                    pstmtPicker.addBatch();
                }

                // 25 Seeded Citizens
                for (int i = 1; i <= 25; i++) {
                    String citId = "CIT00" + i;
                    String email = "citizen" + i + "@wasteconnect.com";
                    String name = "Resident Citizen " + i;
                    String zoneId = "Z" + (((i - 1) % 5) + 1);

                    pstmtUser.setString(1, citId);
                    pstmtUser.setString(2, name);
                    pstmtUser.setString(3, email);
                    pstmtUser.setString(4, citizenHash);
                    pstmtUser.setString(5, "CITIZEN");
                    pstmtUser.setString(6, LocalDateTime.now().minusDays(30).toString());
                    pstmtUser.setInt(7, 1);
                    pstmtUser.addBatch();

                    pstmtCitizen.setString(1, citId);
                    pstmtCitizen.setInt(2, 5 + (i % 7)); // streak
                    pstmtCitizen.setDouble(3, 85.0 + (i % 15)); // score
                    pstmtCitizen.setInt(4, 120 + i * 15); // points
                    pstmtCitizen.setString(5, "House #" + (100 + i) + ", Street " + (i % 8 + 1) + ", " + zoneId);
                    pstmtCitizen.setString(6, "+91-91234" + String.format("%05d", 2000 + i));
                    pstmtCitizen.setString(7, zoneId);
                    pstmtCitizen.setString(8, "HH" + String.format("%03d", i));
                    pstmtCitizen.addBatch();
                }

                pstmtUser.executeBatch();
                pstmtAdmin.executeBatch();
                pstmtCollector.executeBatch();
                pstmtPicker.executeBatch();
                pstmtCitizen.executeBatch();
            }

            // 5. 300+ Households across the 5 zones
            Random rng = new Random(42);
            double[][] zoneCenters = {
                    {12.9716, 77.5946},
                    {12.9810, 77.6050},
                    {12.9352, 77.6946},
                    {12.9141, 77.6329},
                    {12.9902, 77.5520}
            };
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO households VALUES (?, ?, ?, ?, ?, ?);")) {
                for (int i = 1; i <= 350; i++) {
                    int zoneIdx = (i - 1) % 5;
                    String zoneId = "Z" + (zoneIdx + 1);
                    String hhId = "HH" + String.format("%04d", i);
                    String citId = (i <= 25) ? ("CIT00" + i) : null;
                    double lat = zoneCenters[zoneIdx][0] + (rng.nextDouble() - 0.5) * 0.04;
                    double lng = zoneCenters[zoneIdx][1] + (rng.nextDouble() - 0.5) * 0.04;
                    String address = "House #" + i + ", Block " + ((char) ('A' + (i % 6))) + ", Sector " + zoneId;

                    pstmt.setString(1, hhId);
                    pstmt.setString(2, zoneId);
                    pstmt.setString(3, citId);
                    pstmt.setString(4, address);
                    pstmt.setDouble(5, lat);
                    pstmt.setDouble(6, lng);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            // 6. 1,000+ Smart Bins distributed across 5 zones
            String[] wasteTypes = {"WET", "DRY", "HAZARDOUS", "E_WASTE"};
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO bins VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
                for (int i = 1; i <= 1050; i++) {
                    int zoneIdx = (i - 1) % 5;
                    String zoneId = "Z" + (zoneIdx + 1);
                    String binId = "BIN" + String.format("%04d", i);
                    String binCode = "WC-" + zoneId + "-" + String.format("%04d", i);
                    double lat = zoneCenters[zoneIdx][0] + (rng.nextDouble() - 0.5) * 0.045;
                    double lng = zoneCenters[zoneIdx][1] + (rng.nextDouble() - 0.5) * 0.045;
                    String wasteType = wasteTypes[i % 4];
                    double capacity = (i % 4 == 0) ? 240.0 : (i % 4 == 1 ? 400.0 : 660.0);

                    // Skew market zones to have higher fill levels
                    double fill = 20.0 + rng.nextDouble() * 75.0;
                    if (zoneIdx == 0 || zoneIdx == 4) { // market zones
                        fill = Math.min(99.0, fill + 15.0);
                    }
                    String status = fill >= 80.0 ? "WARNING" : "NORMAL";
                    if (i % 80 == 0) {
                        fill = 99.5;
                        status = "OVERFLOW";
                    }

                    pstmt.setString(1, binId);
                    pstmt.setString(2, binCode);
                    pstmt.setString(3, zoneId);
                    pstmt.setDouble(4, lat);
                    pstmt.setDouble(5, lng);
                    pstmt.setString(6, wasteType);
                    pstmt.setDouble(7, capacity);
                    pstmt.setDouble(8, Math.round(fill * 10.0) / 10.0);
                    pstmt.setString(9, LocalDateTime.now().minusHours(rng.nextInt(48)).toString());
                    pstmt.setString(10, status);
                    pstmt.setInt(11, (fill > 85.0 && rng.nextBoolean()) ? 1 : 0);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            // 7. 30 Days of Historical Segregation Logs & Reward Transactions
            try (PreparedStatement pstmtLog = conn.prepareStatement("INSERT INTO segregation_logs VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
                 PreparedStatement pstmtRew = conn.prepareStatement("INSERT INTO reward_transactions VALUES (?, ?, ?, ?, ?, ?);")) {
                for (int i = 1; i <= 200; i++) {
                    String logId = "SLOG" + String.format("%04d", i);
                    String citId = "CIT00" + ((i % 25) + 1);
                    String colId = "COL00" + ((i % 10) + 1);
                    String binId = "BIN00" + ((i % 50) + 1);
                    String quality = (i % 10 == 0) ? "MIXED" : ((i % 4 == 0) ? "PARTIAL" : "GOOD");
                    double weight = 2.0 + rng.nextDouble() * 6.0;
                    int points = quality.equals("GOOD") ? 10 : (quality.equals("PARTIAL") ? 3 : -5);
                    LocalDateTime time = LocalDateTime.now().minusDays(rng.nextInt(30)).minusHours(rng.nextInt(24));

                    pstmtLog.setString(1, logId);
                    pstmtLog.setString(2, citId);
                    pstmtLog.setString(3, colId);
                    pstmtLog.setString(4, binId);
                    pstmtLog.setString(5, quality);
                    pstmtLog.setDouble(6, Math.round(weight * 10.0) / 10.0);
                    pstmtLog.setInt(7, points);
                    pstmtLog.setString(8, time.toString());
                    pstmtLog.addBatch();

                    String rewId = "REW" + String.format("%04d", i);
                    pstmtRew.setString(1, rewId);
                    pstmtRew.setString(2, citId);
                    pstmtRew.setString(3, points >= 0 ? "EARNED" : "PENALTY");
                    pstmtRew.setInt(4, points);
                    pstmtRew.setString(5, "Segregation evaluation: " + quality + " (" + String.format("%.1f", weight) + " kg)");
                    pstmtRew.setString(6, time.toString());
                    pstmtRew.addBatch();
                }
                pstmtLog.executeBatch();
                pstmtRew.executeBatch();
            }

            // 8. Sample Complaints and Timeline Events
            try (PreparedStatement pstmtCmp = conn.prepareStatement("INSERT INTO complaints VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
                 PreparedStatement pstmtEvt = conn.prepareStatement("INSERT INTO complaint_events VALUES (?, ?, ?, ?, ?, ?);")) {
                String[][] sampleComplaints = {
                        {"CMP001", "CIT001", "OVERFLOWING_BIN", "Bin overflowing on 4th Main road corner", "12.9720", "77.5950", "Z1", "OPEN", null, LocalDateTime.now().minusHours(3).toString(), null, ""},
                        {"CMP002", "CIT002", "MISSED_PICKUP", "Scheduled morning door-to-door pickup was missed", "12.9815", "77.6055", "Z2", "ASSIGNED", "COL002", LocalDateTime.now().minusHours(5).toString(), null, ""},
                        {"CMP003", "CIT003", "ILLEGAL_DUMPING", "Piles of construction rubble dumped near park entrance", "12.9360", "77.6950", "Z3", "IN_PROGRESS", "COL003", LocalDateTime.now().minusHours(1).toString(), null, ""},
                        {"CMP004", "CIT004", "DAMAGED_BIN", "Cover lid broken and sensor detached", "12.9150", "77.6335", "Z4", "RESOLVED", "COL004", LocalDateTime.now().minusDays(1).toString(), LocalDateTime.now().minusHours(18).toString(), "Lid replaced and sensor recalibrated"},
                        {"CMP005", "CIT005", "OVERFLOWING_BIN", "Severe accumulation after weekend market", "12.9730", "77.5960", "Z1", "ESCALATED", "COL001", LocalDateTime.now().minusHours(8).toString(), null, "Escalated to Zonal Supervisor due to SLA breach"}
                };
                for (String[] c : sampleComplaints) {
                    pstmtCmp.setString(1, c[0]);
                    pstmtCmp.setString(2, c[1]);
                    pstmtCmp.setString(3, c[2]);
                    pstmtCmp.setString(4, c[3]);
                    pstmtCmp.setDouble(5, Double.parseDouble(c[4]));
                    pstmtCmp.setDouble(6, Double.parseDouble(c[5]));
                    pstmtCmp.setString(7, c[6]);
                    pstmtCmp.setString(8, c[7]);
                    pstmtCmp.setString(9, c[8]);
                    pstmtCmp.setString(10, c[9]);
                    pstmtCmp.setString(11, c[10]);
                    pstmtCmp.setString(12, c[11]);
                    pstmtCmp.addBatch();

                    // Initial event
                    pstmtEvt.setString(1, "EVT-" + c[0] + "-1");
                    pstmtEvt.setString(2, c[0]);
                    pstmtEvt.setString(3, null);
                    pstmtEvt.setString(4, "OPEN");
                    pstmtEvt.setString(5, c[9]);
                    pstmtEvt.setString(6, "Complaint lodged by resident");
                    pstmtEvt.addBatch();

                    if (!c[7].equals("OPEN")) {
                        pstmtEvt.setString(1, "EVT-" + c[0] + "-2");
                        pstmtEvt.setString(2, c[0]);
                        pstmtEvt.setString(3, "OPEN");
                        pstmtEvt.setString(4, c[7]);
                        pstmtEvt.setString(5, LocalDateTime.now().minusMinutes(30).toString());
                        pstmtEvt.setString(6, "Status progressed to " + c[7]);
                        pstmtEvt.addBatch();
                    }
                }
                pstmtCmp.executeBatch();
                pstmtEvt.executeBatch();
            }

            // 9. Sample Recyclable Listings
            try (PreparedStatement pstmtRec = conn.prepareStatement("INSERT INTO recyclable_listings VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
                String[][] listings = {
                        {"LST001", "CIT001", "DRY", "15.0", "House #101, Street 2, Z1", "12.9718", "77.5948", "AVAILABLE", null, LocalDateTime.now().minusHours(4).toString(), null, "150.0"},
                        {"LST002", "CIT002", "DRY", "25.0", "House #102, Street 3, Z2", "12.9812", "77.6052", "CLAIMED", "WPK001", LocalDateTime.now().minusHours(6).toString(), null, "250.0"},
                        {"LST003", "CIT003", "E_WASTE", "8.0", "House #103, Street 4, Z3", "12.9355", "77.6948", "AVAILABLE", null, LocalDateTime.now().minusHours(2).toString(), null, "240.0"}
                };
                for (String[] l : listings) {
                    pstmtRec.setString(1, l[0]);
                    pstmtRec.setString(2, l[1]);
                    pstmtRec.setString(3, l[2]);
                    pstmtRec.setDouble(4, Double.parseDouble(l[3]));
                    pstmtRec.setString(5, l[4]);
                    pstmtRec.setDouble(6, Double.parseDouble(l[5]));
                    pstmtRec.setDouble(7, Double.parseDouble(l[6]));
                    pstmtRec.setString(8, l[7]);
                    pstmtRec.setString(9, l[8]);
                    pstmtRec.setString(10, l[9]);
                    pstmtRec.setString(11, l[10]);
                    pstmtRec.setDouble(12, Double.parseDouble(l[11]));
                    pstmtRec.addBatch();
                }
                pstmtRec.executeBatch();
            }

            // 10. Sample Citizen Notifications
            try (PreparedStatement pstmtNotif = conn.prepareStatement("INSERT INTO notifications VALUES (?, ?, ?, ?, ?, ?);")) {
                String[][] notifs = {
                        {"NTF001", "CIT001", "Segregation Bonus", "Congratulations! You earned +10 reward points for perfect waste segregation.", "0", LocalDateTime.now().minusHours(2).toString()},
                        {"NTF002", "CIT001", "Upcoming Pickup", "Route vehicle KA-01-WC-1001 is estimated to arrive in your sector at 10:30 AM.", "0", LocalDateTime.now().minusHours(1).toString()},
                        {"NTF003", "CIT002", "Recyclable Claimed", "Waste picker Eco Collector 1 has accepted your 25kg dry recyclable listing.", "1", LocalDateTime.now().minusHours(5).toString()}
                };
                for (String[] n : notifs) {
                    pstmtNotif.setString(1, n[0]);
                    pstmtNotif.setString(2, n[1]);
                    pstmtNotif.setString(3, n[2]);
                    pstmtNotif.setString(4, n[3]);
                    pstmtNotif.setInt(5, Integer.parseInt(n[4]));
                    pstmtNotif.setString(6, n[5]);
                    pstmtNotif.addBatch();
                }
                pstmtNotif.executeBatch();
            }

            conn.commit();
            conn.setAutoCommit(true);
            AppLogger.info("Database seeding successfully completed.");
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                AppLogger.severe("Rollback failed during database seed", rollbackEx);
            }
            throw new DataAccessException("Failed to seed initial database records", e);
        }
    }
}
