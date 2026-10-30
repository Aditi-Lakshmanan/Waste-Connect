# OOP Pillars Coverage

## Abstraction

- `com.wasteconnect.model.User` defines the abstract role contract and dashboard permission matrix.
- `com.wasteconnect.model.Waste` captures the abstract waste reward and handling contract.

## Encapsulation

- `User`, `Waste`, `Bin`, and `Citizen` hide internal state behind validated setters and protected accessors.
- `Location` and `RouteStop` serve as immutable value objects.

## Inheritance

- `User` is extended by `Citizen`, `Collector`, `Admin`, and `WastePicker`.
- `Waste` is extended by `WetWaste`, `DryWaste`, `HazardousWaste`, and `EWaste`.
- `Vehicle` is extended by `SmallVehicle` and `LargeTruck`.

## Polymorphism

- `Waste.calculateRewardPoints()` and `getHandlingInstructions()` vary by subtype.
- `User.getDashboardView()` and `getPermissions()` are implemented per role.
- Strategy routing implementations such as `FixedScheduleStrategy` and `PriorityStrategy` are selected at runtime.

## Design patterns and layered architecture

- `DatabaseConnection` implements the singleton pattern.
- `RoutingStrategy` plus strategy implementations provide the strategy pattern.
- `BaseDao<T, ID>` provides the generic repository abstraction used by the DAO layer.
