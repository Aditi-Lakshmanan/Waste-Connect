# WasteConnect – Smart Waste Collection Management System

WasteConnect is a comprehensive Java-based municipal operations and smart waste management platform built with JavaFX, embedded H2 database persistence, and an event-driven architecture. It enforces strict Object-Oriented Programming (OOP) principles, thread-safe service transactions, role-based access control, and dynamic route optimization.

## Quick Start

### Prerequisites
- Java 17+ (JDK / OpenJDK)
- Apache Maven 3.8+

### Build and Run

```bash
# 1. Compile the project
mvn clean compile

# 2. Run the test suite (47 comprehensive tests covering unit, DAO, and E2E flows)
mvn test

# 3. Launch the JavaFX Application
mvn javafx:run
```

---

## Seed Accounts & Authentication

The application enforces strict **Role-Based Access Control (RBAC)**. Public user registration is disabled; all user accounts are created and managed exclusively by Administrators via the Admin Workspace.

| Role | Email | Password | Primary Functions |
|---|---|---|---|
| **Administrator** | `admin@wasteconnect.com` | `Admin@123` | City fleet, zones, smart bins, collector accounts, route planning, SLA & complaints, picker verification, analytics, reports |
| **Waste Collector** | `collector1@wasteconnect.com` | `Collect@123` | Daily route navigation, per-stop collection recording, segregation scoring, road obstruction replanning, complaint resolution |
| **Household / Citizen** | `citizen1@wasteconnect.com` | `Citizen@123` | Special collection requests, reward wallet & voucher redemption, complaints & timeline, recyclable listings, segregation guide, leaderboard |
| **Waste Picker** | `picker1@wasteconnect.com` | `Picker@123` | Geolocation-sorted nearby recyclable offers, offer claiming, measured collection completion, lifetime earnings tracking |

---

## End-to-End Operational Workflows (T1 – T8)

WasteConnect implements and validates 8 core end-to-end municipal flows, verified by automated integration test suite `EndToEndFlowsTest`:

### T1: Administrator Account Provisioning & Role Authentication
- Administrators create verified Citizen, Collector, and WastePicker profiles.
- Password hashing is enforced using salted SHA-256 (`PasswordHasher`).
- All created users authenticate securely through the unified login portal with role validation. Duplicate emails are strictly rejected with `DuplicateEntryException`.

### T2: Smart Route Generation and Collector Assignment
- Administrators select a municipal zone, fleet vehicle, collector, and routing algorithm (`PriorityStrategy` using IoT urgency scores or `FixedScheduleStrategy`).
- The system computes optimal routes, sequences stops, and stores the assignment.
- Assigned collectors instantly receive a notification and see the route loaded into their daily workspace.

### T3: Stop Collection, Segregation Quality & Citizen Reward Points
- Collectors arrive at collection stops, evaluate household segregation quality (`GOOD`, `PARTIAL`, `MIXED`), and record actual weight.
- Good segregation rewards citizen accounts with reward points and updates their streak.
- Transparent transaction entries are written to the points ledger (`RewardTransactionDao`), and notifications are dispatched via the `EventBus`.

### T4: Citizen Complaint Lifecycle & SLA Escalation
- Citizens log municipal complaints (e.g., overflowing bins, missed pickups) with GPS coordinates.
- Complaints are automatically assigned to an active collector in the matching zone.
- Collectors resolve complaints with verification remarks; citizens receive resolution alerts and can inspect the timeline.
- An automated background scheduler scans and escalates SLA-breached complaints.

### T5: Recyclable Marketplace Handoff (Citizen → Picker → Earnings)
- Households offer recyclable commodities (dry paper/plastic, e-waste) with estimated weight.
- Verified informal waste pickers browse offers sorted by proximity to their current GPS coordinates.
- Pickers atomically claim listings and finalize pickups with measured weights.
- Picker earnings increase instantly, and the household receives bonus green points.

### T6: Route Obstruction Handling & Dynamic Replanning
- If a collector encounters a blocked road or obstacle on route, they submit an obstruction report with the stop sequence.
- The routing engine dynamically replans the remaining stops, bypasses the obstacle, updates the sequence, and emits a `RouteReplanned` event.

### T7: Citizen Points Wallet & Voucher Redemption
- Citizens redeem earned green points for municipal benefits (bus passes, utility rebates, retail discounts).
- Transactions run within atomic DB transactions: points are decremented, a unique redemption verification code is issued, and insufficient balances are strictly rejected with `ValidationException`.

### T8: Admin-Only Security Guard
- Access to user creation (`registerCitizen`, `registerWastePicker`, `saveCollector`) is protected by `ensureAdmin()` guards.
- Anonymous or non-admin users attempting to invoke user registration endpoints are immediately blocked with `AuthorizationException`.

---

## Architecture & OOP Pillars

- **Encapsulation**: Domain models (`Bin`, `User`, `Route`, `Citizen`) enforce strict validation invariants, private fields with defensive copies, and controlled status transitions.
- **Inheritance**: Base `User` class extended by `Admin`, `Citizen`, `Collector`, and `WastePicker`. Specialized vehicles (`SmallVehicle`, `LargeTruck`) inherit from abstract `Vehicle`.
- **Polymorphism & Design Patterns**:
  - **Strategy Pattern**: `RoutingStrategy` interface with `PriorityStrategy` (Dijkstra/IoT urgency) and `FixedScheduleStrategy`.
  - **Factory Pattern**: `UserFactory`, `VehicleFactory`, and `WasteFactory`.
  - **Observer / EventBus Pattern**: Singleton typed `EventBus` enabling real-time UI updates across screens without polling.
  - **DAO & Singleton Pattern**: Thread-safe `DatabaseConnection` managing transactional SQLite/H2 connections.
  - **Asynchronous UI & Background Loading**: Thread-safe `BackgroundLoader` leveraging `javafx.concurrent.Task` on daemon threads to offload heavy operations (hotspot clustering, route benchmark simulations, CSV report streaming) while keeping the JavaFX application thread responsive.

---

## Database Architecture

- **Active Database**: Embedded H2 (`jdbc:h2:file:./data/wasteconnect;DB_CLOSE_DELAY=-1`).
- **Data File**: `data/wasteconnect.mv.db` (auto-seeded with default zones, vehicles, smart bins, and accounts on first launch).
- **DDL Schema**: Located at `src/main/resources/schema.sql`.

---

## Verification & Test Results

Run all test suites with:
```bash
mvn test
```

**Results: 47 tests run, 0 failures, 0 errors, 0 skipped (100% PASS)**
- `EndToEndFlowsTest`: 8 tests covering T1–T8 operational scenarios
- `DaoAndServiceTest`: 22 tests covering CRUD, transactions, filtering, and analytics
- `OopPillarsTest`: 7 tests verifying encapsulation, polymorphism, and inheritance
- `EngineTest`: 4 tests verifying priority scoring, routing algorithms, and hotspot detection
- `UtilTest`: 3 tests verifying password hashing, email validation, and phone validation
- `EventBusTest`: 2 tests verifying typed event dispatch and subscription lifecycle
- `NotificationServiceTest`: 1 test verifying notification persistence and unread count
