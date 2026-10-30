# WasteConnect – Smart Waste Collection Management System

WasteConnect is a Java-based demonstration project for a smart waste collection and municipal operations platform. It uses a layered DAO/service architecture, a JavaFX desktop interface, and a local JDBC database. The application has working role-based workflows, but it does not yet implement every feature in the complete product specification. See [docs/AUDIT.md](docs/AUDIT.md) for audited status and remaining work.

## Run

```bash
cd e:\WasteConnect
set JAVA_HOME="C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\jbr"
set PATH=%JAVA_HOME%\bin;%PATH%
"C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin\mvn.cmd" clean compile
"C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin\mvn.cmd" javafx:run
```

## Default accounts

- Admin: admin@wasteconnect.com / Admin@123
- Collector: collector1@wasteconnect.com / Collect@123
- Citizen: citizen1@wasteconnect.com / Citizen@123
- Waste picker: picker1@wasteconnect.com / Picker@123

## Features

- Role-aware login for Administrator, Household/Citizen, Waste Collector, and Waste Picker
- Household pickup requests, upcoming-request display, complaint submission, points wallet/redemption, and recyclable offers
- Collector route stop recording, segregation evaluation, citizen reward updates, blocked-road replanning, and complaint resolution
- Admin household, zone, bin, vehicle, collector, complaint, picker, and analytics views
- Admin route generation and assignment using Fixed or Priority routing strategies
- Admin household and zone/vehicle create, edit, and delete workflows
- Admin smart-bin search, zone/type filters, pagination, and CRUD
- Admin collector account creation with hashed credentials, assignment edits, deactivation, and deletion
- Admin smart-bin simulator controls for start/stop, speed, festival surge, and confirmed reset
- Analytics computed from persisted routes, segregation logs, complaint statuses, and bin waste types
- Household searchable segregation guide, complaint timeline, leaderboard, and redemption codes
- Role-specific notification centres with unread counts and read-state actions
- Admin complaint assignment to a collector in the complaint's zone
- Household complaints are auto-assigned to a collector in the same zone when one is available
- Admin waste-picker approval/rejection with notifications
- Typed EventBus for selected service events and role/admin workspace refreshes
- Waste picker distance-sorted listing claims and completion with picker earnings and household point credits
- Database seeding for zones, households, bins, vehicles, and default users
- JUnit coverage for services, routing, route dispatch, and EventBus behavior

Several requested features remain unimplemented, including the live map, Kanban/SLA scheduler, verified simulator persistence/reset behavior, reports, settings UI, charts, background data loading, notification centre, and complete cross-role end-to-end scenarios. Household/bin/zone/vehicle/collector admin CRUD controls now exist, but JavaFX dialogs have not all had manual click-through verification. The EventBus currently covers only the event publications listed in its implementation; it is not yet a complete application-wide implementation of every requested event.

### Database location

The active database URL is `jdbc:h2:file:./data/wasteconnect;AUTO_SERVER=TRUE`, so the H2 database file is `data/wasteconnect.mv.db` relative to the project root. It stores the users, households, zones, bins, vehicles, routes and stops, pickups, complaints, rewards, recyclable listings, notifications, and settings. `data/wasteconnect.db` and its `-wal`/`-shm` companions are SQLite files from a previous/alternate setup; the current code does not select them. Keep them until you confirm their data is no longer needed.

## Verified demo workflows

1. Launch with `mvn javafx:run` and sign in with one of the accounts above.
2. As admin, open **Routes**, choose a zone, vehicle, collector, and routing strategy, then generate and assign a route.
3. Sign in as the assigned collector and open **Route** to see persisted stops; record a stop with quality and weight.
4. As a household user, submit a pickup request or complaint, offer recyclables, and view the stored requests and points ledger.
5. As a verified picker, claim an available listing and complete it with measured weight.
6. As admin, assign complaints and approve or reject waste-picker accounts.

These are individual verified workflows, not a claim that all requested scenarios S1–S8 have passed end-to-end. Automated coverage currently includes route persistence/collector visibility, picker claim ownership, EventBus delivery/unsubscription, and the existing service and routing tests.

## Notes

This project uses a portable JDBC fallback for execution on this environment while preserving the same service-layer schema and DAO API contracts used by the academic project.
