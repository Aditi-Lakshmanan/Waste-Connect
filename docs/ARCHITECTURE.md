# Architecture Overview

## Layered architecture

The system follows a layered architecture across model, DAO, service, engine, pattern, and UI. The application initializes the database once, seeds baseline records, and routes work through service classes to provide logic and validation.

## Class diagram

```mermaid
classDiagram
    class User
    class Citizen
    class Collector
    class Admin
    class WastePicker
    class Waste
    class Bin
    class Zone
    class Route
    class Complaint
    class RecyclableListing
    class DatabaseConnection
    class AuthService
    class CitizenService
    class CollectorService
    class AdminService
    class WastePickerService
    class RoutingEngine
    class PriorityScoreEngine
    class SmartBinSimulator

    User <|-- Citizen
    User <|-- Collector
    User <|-- Admin
    User <|-- WastePicker
    Waste <|-- WetWaste
    Waste <|-- DryWaste
    Waste <|-- HazardousWaste
    Waste <|-- EWaste

    DatabaseConnection --> AuthService
    CitizenService --> Complaint
    CollectorService --> Route
    AdminService --> Zone
    WastePickerService --> RecyclableListing
    RoutingEngine --> PriorityScoreEngine
    SmartBinSimulator --> Bin
```

## ER diagram

```mermaid
erDiagram
    USERS ||--o{ CITIZENS : contains
    USERS ||--o{ COLLECTORS : contains
    USERS ||--o{ WASTE_PICKERS : contains
    ZONES ||--o{ BINS : hosts
    ZONES ||--o{ HOUSEHOLDS : includes
    USERS ||--o{ COMPLAINTS : raises
    USERS ||--o{ REWARD_TRANSACTIONS : earns
    CITIZENS ||--o{ SEGREGATION_LOGS : records
    ROUTES ||--o{ ROUTE_STOPS : contains
    USERS ||--o{ NOTIFICATIONS : receives
    USERS ||--o{ PICKUPS : schedules
    USERS ||--o{ RECYCLABLE_LISTINGS : creates
```
