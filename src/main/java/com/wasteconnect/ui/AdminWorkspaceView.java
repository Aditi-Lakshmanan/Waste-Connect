package com.wasteconnect.ui;

import com.wasteconnect.model.Bin;
import com.wasteconnect.model.BinStatus;
import com.wasteconnect.model.Collector;
import com.wasteconnect.model.Complaint;
import com.wasteconnect.model.ComplaintStatus;
import com.wasteconnect.model.Household;
import com.wasteconnect.model.User;
import com.wasteconnect.model.Vehicle;
import com.wasteconnect.model.SmallVehicle;
import com.wasteconnect.model.LargeTruck;
import com.wasteconnect.model.VehicleType;
import com.wasteconnect.model.WasteAnalytics;
import com.wasteconnect.model.WastePicker;
import com.wasteconnect.model.Zone;
import com.wasteconnect.model.Route;
import com.wasteconnect.pattern.FixedScheduleStrategy;
import com.wasteconnect.pattern.PriorityStrategy;
import com.wasteconnect.pattern.RoutingStrategy;
import com.wasteconnect.service.AdminService;
import com.wasteconnect.service.NotificationService;
import com.wasteconnect.model.Notification;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;
import com.wasteconnect.pattern.EventBus;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Slider;
import javafx.scene.control.PasswordField;
import javafx.stage.FileChooser;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import com.wasteconnect.model.Location;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.event.ActionEvent;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AdminWorkspaceView extends BorderPane {
    private static final int PAGE_SIZE = 25;

    private final SceneManager sceneManager;
    private final AdminService adminService = new AdminService();
    private final NotificationService notificationService = new NotificationService();
    // Holds the currently displayed Zone objects for edit/delete actions
    private List<Zone> currentZonePage = new ArrayList<>();
    // Holds the currently displayed Vehicle objects for edit/delete actions
    private final VBox navigation = new VBox(8);
    private List<Vehicle> currentVehiclePage = new ArrayList<>();
    // Holds the currently displayed Bin objects for edit/delete actions
    private List<Bin> currentBinPage = new ArrayList<>();



    private final Label moduleTitle = new Label();
    private final Label resultSummary = new Label();
    private final Label pageLabel = new Label();
    private final TextField searchField = new TextField();
    private final TableView<List<String>> table = new TableView<>();
    private final HBox routeControls = new HBox(10);
    private final HBox moduleActionControls = new HBox(10);
    private final HBox binFilterControls = new HBox(10);
    private final HBox simulatorControls = new HBox(10);
    private final ComboBox<String> binZoneFilter = new ComboBox<>();
    private final ComboBox<String> binTypeFilter = new ComboBox<>();
    private final ComboBox<Zone> routeZoneSelector = new ComboBox<>();
    private final ComboBox<Vehicle> routeVehicleSelector = new ComboBox<>();
    private final ComboBox<Collector> routeCollectorSelector = new ComboBox<>();
    private final ComboBox<String> routeStrategySelector = new ComboBox<>();
    private final Button previousButton = new Button("Previous");
    private final Button nextButton = new Button("Next");
    private final Button notificationsButton = new Button("Notifications");
    private User adminUser;
    private final List<AutoCloseable> eventSubscriptions = new ArrayList<>();
    private final AtomicBoolean eventRefreshQueued = new AtomicBoolean();
    private final ScheduledExecutorService slaScheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "wasteconnect-complaint-sla");
        thread.setDaemon(true);
        return thread;
    });
    private String currentModule = "overview";
    private int currentPage;
    private int resultCount;

    public AdminWorkspaceView(SceneManager sceneManager, User user) {
        this.sceneManager = sceneManager;
        this.adminUser = user;
        setLeft(buildSidebar(user));
        setTop(buildToolbar());
        setCenter(buildContent());
        showModule("overview");
        initializeRouteControls();
        initializeSimulatorControls();
        subscribeToEvents();
        refreshNotificationCount();
        slaScheduler.scheduleAtFixedRate(() -> {
            try {
                int escalated = adminService.escalateOverdueComplaints();
                if (escalated > 0) refreshFromEvent();
            } catch (RuntimeException exception) {
                com.wasteconnect.util.AppLogger.severe("Complaint SLA scan failed", exception);
            }
        }, 15, 60, TimeUnit.SECONDS);
        sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                slaScheduler.shutdownNow();
                unsubscribeFromEvents();
            }
        });
    }

    private VBox buildSidebar(User user) {
        VBox sidebar = new VBox(18);
        sidebar.setPadding(new Insets(24, 16, 20, 16));
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #142019;");

        Label brand = new Label("WasteConnect");
        brand.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #83D59D;");
        Label identity = new Label("ADMIN  /  " + user.getName());
        identity.setStyle("-fx-font-size: 11px; -fx-text-fill: #A2B1A7;");

        navigation.setFillWidth(true);
        addNavigationItem("Overview", "overview");
        addNavigationItem("Households", "households");
        addNavigationItem("Zones", "zones");
        addNavigationItem("Smart bins", "bins");
        addNavigationItem("Vehicles", "vehicles");
        addNavigationItem("Collectors", "collectors");
        addNavigationItem("Routes", "routes");
        addNavigationItem("Complaints", "complaints");
        addNavigationItem("Waste pickers", "pickers");
        addNavigationItem("Hotspots", "hotspots");
        addNavigationItem("Benchmark", "benchmark");
        addNavigationItem("Analytics", "analytics");
        addNavigationItem("Simulator", "simulator");
        addNavigationItem("Reports", "reports");

        Label version = new Label("MUNICIPAL OPERATIONS");
        version.setStyle("-fx-font-size: 10px; -fx-text-fill: #718379;");
        VBox.setVgrow(navigation, Priority.ALWAYS);
        sidebar.getChildren().addAll(brand, identity, navigation, version);
        return sidebar;
    }

    private void addNavigationItem(String label, String module) {
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(event -> showModule(module));
        button.setStyle("-fx-background-color: transparent; -fx-text-fill: #C8D6CD; -fx-padding: 10 12; -fx-background-radius: 7;");
        navigation.getChildren().add(button);
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(14);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(18, 24, 16, 24));
        toolbar.setStyle("-fx-background-color: #101713; -fx-border-color: transparent transparent #29382F transparent;");

        moduleTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #E5EEE8;");
        searchField.setPromptText("Search current module");
        searchField.setPrefWidth(260);
        searchField.setOnAction(event -> refreshCurrentModule());
        Button searchButton = new Button("Search");
        searchButton.setOnAction(event -> refreshCurrentModule());
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> refreshCurrentModule());
        notificationsButton.setOnAction(event -> showNotificationsDialog());
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> sceneManager.showLogout());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().addAll(moduleTitle, spacer, searchField, searchButton, refreshButton, notificationsButton, logoutButton);
        return toolbar;
    }

    private VBox buildContent() {
        VBox content = new VBox(14);
        content.setPadding(new Insets(18, 24, 18, 24));
        resultSummary.setStyle("-fx-text-fill: #A2B1A7;");
        routeControls.setVisible(false);
        routeControls.setManaged(false);
        binFilterControls.setVisible(false);
        binFilterControls.setManaged(false);
        simulatorControls.setVisible(false);
        simulatorControls.setManaged(false);
        moduleActionControls.setVisible(false);
        moduleActionControls.setManaged(false);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.widthProperty().addListener((observable, oldWidth, newWidth) -> resizeFinalColumn(newWidth.doubleValue()));
        table.setPlaceholder(new Label("No records found"));

        HBox pager = new HBox(10);
        pager.setAlignment(Pos.CENTER_RIGHT);
        previousButton.setOnAction(event -> {
            if (currentPage > 0) {
                currentPage--;
                refreshCurrentModule();
            }
        });
        nextButton.setOnAction(event -> {
            if ((currentPage + 1) * PAGE_SIZE < resultCount) {
                currentPage++;
                refreshCurrentModule();
            }
        });
        pager.getChildren().addAll(pageLabel, previousButton, nextButton);
        content.getChildren().addAll(routeControls, binFilterControls, simulatorControls, moduleActionControls,
            resultSummary, table, pager);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private void showModule(String module) {
        currentModule = module;
        currentPage = 0;
        searchField.clear();
        refreshCurrentModule();
    }

    private void refreshCurrentModule() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        routeControls.setVisible("routes".equals(currentModule));
        routeControls.setManaged("routes".equals(currentModule));
        binFilterControls.setVisible("bins".equals(currentModule));
        binFilterControls.setManaged(binFilterControls.isVisible());
        simulatorControls.setVisible("simulator".equals(currentModule));
        simulatorControls.setManaged(simulatorControls.isVisible());
        moduleActionControls.setVisible("households".equals(currentModule) || "zones".equals(currentModule)
            || "bins".equals(currentModule) || "vehicles".equals(currentModule) || "collectors".equals(currentModule)
            || "complaints".equals(currentModule) || "pickers".equals(currentModule));
        moduleActionControls.setManaged(moduleActionControls.isVisible());
        if ("households".equals(currentModule)) {
            configureHouseholdActions();
            showHouseholds(query);
            return;
        }
        if ("routes".equals(currentModule)) {
            showRoutes(query);
            return;
        }

        List<String> headers;
        List<List<String>> rows;
        switch (currentModule) {
            case "zones" -> {
                moduleTitle.setText("Zones");
                headers = List.of("Zone ID", "Name", "Code", "Center", "Market area");
                rows = adminService.getAllZones().stream().map(zone -> List.of(
                        zone.getId(), zone.getName(), zone.getCode(),
                        coordinates(zone.getCenterLat(), zone.getCenterLng()), Boolean.toString(zone.isMarketArea())))
                        .toList();
                    configureZoneActions();
            }
            case "bins" -> {
                moduleTitle.setText("Smart bins");
                configureBinActions();
                showBins(query);
                return;
            }
            case "simulator" -> {
                showSimulator(query);
                return;
            }
            case "reports" -> {
                showReports();
                return;
            }
            case "vehicles" -> {
                moduleTitle.setText("Vehicles");
                headers = List.of("Vehicle ID", "Registration", "Type", "Capacity", "Active");
                rows = adminService.getAllVehicles().stream().map(vehicle -> List.of(
                        vehicle.getId(), vehicle.getRegistrationNumber(), vehicle.getVehicleType().name(),
                        String.format("%.0f kg", vehicle.getCapacityKg()), Boolean.toString(vehicle.isActive())))
                        .toList();
                    configureVehicleActions();
            }
            case "collectors" -> {
                moduleTitle.setText("Collectors");
                headers = List.of("Collector ID", "Name", "Email", "Zone", "Vehicle");
                rows = adminService.getAllCollectors().stream().map(collector -> List.of(
                        collector.getId(), collector.getName(), collector.getEmail(),
                        valueOrDash(collector.getAssignedZoneId()), valueOrDash(collector.getAssignedVehicleId())))
                        .toList();
                    configureCollectorActions();
            }
            case "complaints" -> {
                moduleTitle.setText("Complaints");
                headers = List.of("Complaint ID", "Citizen", "Type", "Zone", "Status", "SLA remaining", "Assigned collector");
                rows = adminService.getAllComplaints().stream().map(complaint -> List.of(
                    complaint.getId(), complaint.getCitizenId(), complaint.getComplaintType().getLabel(),
                    complaint.getZoneId(), complaint.getStatus().name(),
                    String.format("%.1f h", Math.max(0.0, complaint.getComplaintType().getSlaHours() - complaint.getHoursElapsed())),
                    valueOrDash(complaint.getAssignedCollectorId())))
                        .toList();
                configureComplaintActions();
            }
            case "pickers" -> {
                moduleTitle.setText("Waste pickers");
                headers = List.of("Picker ID", "Name", "Email", "Phone", "Verified", "Collected");
                rows = adminService.getAllPickers().stream().map(picker -> List.of(
                        picker.getId(), picker.getName(), picker.getEmail(), valueOrDash(picker.getPhone()),
                        picker.isVerified() ? "Verified" : "Pending",
                        String.format("%.1f kg", picker.getTotalKgCollected())))
                        .toList();
                    configurePickerActions();
            }
            case "hotspots" -> {
                moduleTitle.setText("Garbage Vulnerable Hotspots");
                headers = List.of("Zone", "Risk Level", "Score", "Complaints", "Overflows", "Location", "Recommendation");
                rows = adminService.detectHotspots().stream().map(h -> List.of(
                        h.zoneName(), h.riskLevel(), String.format("%.1f", h.riskScore()),
                        Integer.toString(h.complaintCount()), Integer.toString(h.overflowCount()),
                        coordinates(h.latitude(), h.longitude()), h.recommendation()))
                        .toList();
            }
            case "benchmark" -> {
                moduleTitle.setText("Route Optimization Benchmark");
                headers = List.of("Metric / Strategy", "Fixed-Schedule (Traditional)", "Priority-Based (WasteConnect)", "Advantage");
                Zone firstZone = adminService.getAllZones().isEmpty() ? null : adminService.getAllZones().get(0);
                if (firstZone != null) {
                    try {
                        var comp = adminService.runBenchmark(firstZone.getId());
                        rows = List.of(
                                List.of("Total Distance", String.format("%.1f km", comp.fixedResult().totalDistanceKm()),
                                        String.format("%.1f km", comp.priorityResult().totalDistanceKm()),
                                        String.format("%.1f%% saved", comp.getDistanceSavedPercentage())),
                                List.of("Stops Evaluated", Integer.toString(comp.fixedResult().totalStopsEvaluated()),
                                        Integer.toString(comp.priorityResult().totalStopsEvaluated()), "Full Coverage"),
                                List.of("Missed Pickups", Integer.toString(comp.fixedResult().missedPickups()),
                                        Integer.toString(comp.priorityResult().missedPickups()),
                                        String.format("%d avoided", comp.getMissedPickupsReduction())),
                                List.of("Overflows Prevented", Integer.toString(comp.fixedResult().overflowIncidentsPrevented()),
                                        Integer.toString(comp.priorityResult().overflowIncidentsPrevented()), "Proactive Emptying"),
                                List.of("Fill at Collection", String.format("%.1f%%", comp.fixedResult().averageFillAtCollection()),
                                        String.format("%.1f%%", comp.priorityResult().averageFillAtCollection()),
                                        String.format("+%.1f%% efficiency", comp.getEfficiencyGainPercentage())),
                                List.of("Execution Time", comp.fixedResult().executionTimeMs() + " ms",
                                        comp.priorityResult().executionTimeMs() + " ms", "Sub-second Optimal")
                        );
                    } catch (Exception e) {
                        rows = List.of(List.of("Benchmark Status", "Run Error: " + e.getMessage(), "-", "-"));
                    }
                } else {
                    rows = List.of();
                }
            }
            case "analytics" -> {
                moduleTitle.setText("Analytics");
                headers = List.of("Measure", "Value");
                WasteAnalytics analytics = adminService.getMunicipalAnalytics();
                rows = List.of(
                        List.of("Collection efficiency", String.format("%.1f%%", analytics.collectionEfficiencyPct())),
                        List.of("Average segregation rate", String.format("%.1f%%", analytics.averageSegregationRatePct())),
                        List.of("Complaints resolved", String.format("%.1f%%", analytics.complaintsResolvedPct())),
                        List.of("Wet waste", String.format("%.1f kg", analytics.totalWetKg())),
                        List.of("Dry waste", String.format("%.1f kg", analytics.totalDryKg())),
                        List.of("Hazardous waste", String.format("%.1f kg", analytics.totalHazardousKg())),
                        List.of("E-waste", String.format("%.1f kg", analytics.totalEWasteKg())),
                        List.of("Distance saved", String.format("%.1f km", analytics.estimatedDistanceSavedKm())),
                        List.of("Fuel savings", String.format("INR %.0f", analytics.fuelCostSavedInr())));
            }
            default -> {
                moduleTitle.setText("Operations overview");
                headers = List.of("Module", "Records", "Coverage");
                rows = List.of(
                        List.of("Households", Integer.toString(adminService.getHouseholdsCount("")), "Search and pagination"),
                        List.of("Zones", Integer.toString(adminService.getAllZones().size()), "Municipal service areas"),
                        List.of("Smart bins", Integer.toString(adminService.getBinsCount(null, null, "")), "Fill and status monitoring"),
                        List.of("Vehicles", Integer.toString(adminService.getAllVehicles().size()), "Fleet registry"),
                        List.of("Collectors", Integer.toString(adminService.getAllCollectors().size()), "Staff assignments"),
                        List.of("Complaints", Integer.toString(adminService.getAllComplaints().size()), "Status tracking"),
                        List.of("Waste pickers", Integer.toString(adminService.getAllPickers().size()), "Verification tracking"));
            }
        }

        List<List<String>> filteredRows = query.isEmpty() ? rows : rows.stream()
                .filter(row -> row.stream().anyMatch(value -> value.toLowerCase().contains(query.toLowerCase())))
                .toList();
        resultCount = filteredRows.size();
        currentPage = Math.min(currentPage, Math.max(0, (resultCount - 1) / PAGE_SIZE));
        int fromIndex = Math.min(currentPage * PAGE_SIZE, resultCount);
        int toIndex = Math.min(fromIndex + PAGE_SIZE, resultCount);
        setTable(headers, filteredRows.subList(fromIndex, toIndex));
        resultSummary.setText("Showing " + (resultCount == 0 ? 0 : fromIndex + 1) + "-" + toIndex + " of "
            + resultCount + " records" + (query.isEmpty() ? "" : " matching search"));
        pageLabel.setText("Page " + (currentPage + 1) + " / " + Math.max(1, (resultCount + PAGE_SIZE - 1) / PAGE_SIZE));
        updatePager();
    }

    private void showHouseholds(String query) {
        moduleTitle.setText("Households");
        resultCount = adminService.getHouseholdsCount(query);
        currentPage = Math.min(currentPage, Math.max(0, (resultCount - 1) / PAGE_SIZE));
        List<Household> households = adminService.getHouseholdsPage(currentPage, PAGE_SIZE, query);
        List<List<String>> rows = households.stream().map(household -> List.of(
                household.getId(), household.getZoneId(), valueOrDash(household.getCitizenId()), household.getAddress()))
                .toList();
        setTable(List.of("Household ID", "Zone", "Citizen", "Address"), rows);
        int first = resultCount == 0 ? 0 : currentPage * PAGE_SIZE + 1;
        int last = Math.min((currentPage + 1) * PAGE_SIZE, resultCount);
        resultSummary.setText("Showing " + first + "-" + last + " of " + resultCount + " households");
        pageLabel.setText("Page " + (currentPage + 1) + " / " + Math.max(1, (resultCount + PAGE_SIZE - 1) / PAGE_SIZE));
        updatePager();
    }

    private void initializeRouteControls() {
        routeZoneSelector.getItems().setAll(adminService.getAllZones());
        routeVehicleSelector.getItems().setAll(adminService.getAllVehicles());
        routeCollectorSelector.getItems().setAll(adminService.getAllCollectors());
        routeStrategySelector.getItems().setAll("Priority", "Fixed");
        routeZoneSelector.getSelectionModel().selectFirst();
        routeVehicleSelector.getSelectionModel().selectFirst();
        routeCollectorSelector.getSelectionModel().selectFirst();
        routeStrategySelector.getSelectionModel().selectFirst();
        routeZoneSelector.setPrefWidth(200);
        routeVehicleSelector.setPrefWidth(220);
        routeCollectorSelector.setPrefWidth(230);
        routeStrategySelector.setPrefWidth(140);
        routeZoneSelector.setPromptText("Zone");
        routeVehicleSelector.setPromptText("Vehicle");
        routeCollectorSelector.setPromptText("Collector");
        routeStrategySelector.setPromptText("Strategy");

        Button generate = new Button("Generate and assign route");
        generate.setOnAction(event -> {
            Zone zone = routeZoneSelector.getValue();
            Vehicle vehicle = routeVehicleSelector.getValue();
            Collector collector = routeCollectorSelector.getValue();
            if (zone == null || vehicle == null || collector == null) {
                resultSummary.setText("Choose a zone, vehicle, and collector before generating a route.");
                return;
            }
            RoutingStrategy strategy = "Fixed".equals(routeStrategySelector.getValue())
                    ? new FixedScheduleStrategy() : new PriorityStrategy();
            try {
                Route route = adminService.planRoute(zone.getId(), vehicle.getId(), collector.getId(), strategy);
                showRoutes(searchField.getText() == null ? "" : searchField.getText().trim());
                resultSummary.setText("Route " + route.getId() + " assigned to " + collector.getName()
                        + " with " + route.getStopsCount() + " stops and "
                        + String.format("%.1f km", route.getTotalDistanceKm()) + ".");
            } catch (RuntimeException exception) {
                resultSummary.setText(exception.getMessage() == null ? "Route could not be generated." : exception.getMessage());
            }
        });
        routeZoneSelector.setOnAction(event -> {
            if ("routes".equals(currentModule)) {
                showRoutes(searchField.getText() == null ? "" : searchField.getText().trim());
            }
        });
        routeControls.getChildren().setAll(routeZoneSelector, routeVehicleSelector,
                routeCollectorSelector, routeStrategySelector, generate);
    }

    private void configureComplaintActions() {
        Button assignButton = new Button("Assign to zone collector");
        assignButton.setOnAction(event -> {
            List<String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                resultSummary.setText("Select a complaint to assign.");
                return;
            }
            try {
                String collectorId = adminService.assignComplaintToZoneCollector(selected.get(0));
                refreshCurrentModule();
                resultSummary.setText("Complaint assigned to collector " + collectorId + ".");
            } catch (RuntimeException exception) {
                resultSummary.setText(exception.getMessage() == null ? "Assignment failed." : exception.getMessage());
            }
        });
        ComboBox<ComplaintStatus> statusSelector = new ComboBox<>();
        statusSelector.getItems().setAll(ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED, ComplaintStatus.ESCALATED);
        statusSelector.getSelectionModel().selectFirst();
        TextField statusNote = new TextField();
        statusNote.setPromptText("Required status note");
        statusNote.setPrefWidth(240);
        Button updateStatus = new Button("Update selected complaint");
        updateStatus.setOnAction(event -> {
            List<String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                resultSummary.setText("Select a complaint to update.");
                return;
            }
            try {
                adminService.updateComplaintStatus(selected.get(0), statusSelector.getValue(), statusNote.getText());
                statusNote.clear();
                refreshCurrentModule();
                resultSummary.setText("Complaint status updated.");
            } catch (RuntimeException exception) {
                resultSummary.setText(exception.getMessage() == null ? "Complaint status could not be updated." : exception.getMessage());
            }
        });
        Button scanSla = new Button("Scan overdue now");
        scanSla.setOnAction(event -> {
            int count = adminService.escalateOverdueComplaints();
            refreshCurrentModule();
            resultSummary.setText(count + " overdue complaint(s) escalated.");
        });
        moduleActionControls.getChildren().setAll(assignButton, statusSelector, statusNote, updateStatus, scanSla);
    }

    private void configureBinActions() {
        Button add = new Button("Add bin");
        add.setOnAction(event -> showBinDialog(null));
        Button edit = new Button("Edit selected bin");
        edit.setOnAction(event -> {
            List<String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                resultSummary.setText("Select a bin to edit.");
                return;
            }
            adminService.getBinsPage(0, Integer.MAX_VALUE, null, null, selected.get(0)).stream()
                    .filter(bin -> bin.getId().equals(selected.get(0))).findFirst()
                    .ifPresentOrElse(this::showBinDialog, () -> resultSummary.setText("Selected bin was not found."));
        });
        Button delete = new Button("Delete selected bin");
        delete.setOnAction(event -> {
            List<String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                resultSummary.setText("Select a bin to delete.");
                return;
            }
            try {
                if (adminService.deleteBin(selected.get(0))) {
                    refreshCurrentModule();
                    resultSummary.setText("Bin deleted.");
                } else {
                    resultSummary.setText("Bin was not found or could not be deleted.");
                }
            } catch (RuntimeException exception) {
                resultSummary.setText(exception.getMessage() == null ? "Bin could not be deleted." : exception.getMessage());
            }
        });
        moduleActionControls.getChildren().setAll(add, edit, delete);
    }

    private void initializeBinFilters() {
        binZoneFilter.getItems().setAll("ALL");
        binZoneFilter.getItems().addAll(adminService.getAllZones().stream().map(Zone::getId).toList());
        binTypeFilter.getItems().setAll("ALL", "WET", "DRY", "HAZARDOUS", "E_WASTE");
        binZoneFilter.getSelectionModel().selectFirst();
        binTypeFilter.getSelectionModel().selectFirst();
        binZoneFilter.setPrefWidth(160);
        binTypeFilter.setPrefWidth(160);
        binZoneFilter.setPromptText("Zone");
        binTypeFilter.setPromptText("Waste type");
        Button apply = new Button("Apply filters");
        apply.setOnAction(event -> {
            currentPage = 0;
            refreshCurrentModule();
        });
        binFilterControls.getChildren().setAll(new Label("Zone"), binZoneFilter,
                new Label("Waste type"), binTypeFilter, apply);
    }

    private void showBins(String query) {
        if (binZoneFilter.getItems().isEmpty()) initializeBinFilters();
        String zoneId = binZoneFilter.getValue();
        String wasteType = binTypeFilter.getValue();
        resultCount = adminService.getBinsCount(zoneId, wasteType, query);
        currentPage = Math.min(currentPage, Math.max(0, (resultCount - 1) / PAGE_SIZE));
        List<Bin> bins = adminService.getBinsPage(currentPage, PAGE_SIZE, zoneId, wasteType, query);
        List<List<String>> rows = bins.stream().map(bin -> List.of(
                bin.getId(), bin.getBinCode(), bin.getZoneId(), bin.getWasteType().name(),
                String.format("%.0f%%", bin.getCurrentFillPercentage()), bin.getStatus().name()))
                .toList();
        setTable(List.of("Bin ID", "Bin code", "Zone", "Waste type", "Fill", "Status"), rows);
        int first = resultCount == 0 ? 0 : currentPage * PAGE_SIZE + 1;
        int last = Math.min((currentPage + 1) * PAGE_SIZE, resultCount);
        resultSummary.setText("Showing " + first + "-" + last + " of " + resultCount + " bins");
        pageLabel.setText("Page " + (currentPage + 1) + " / " + Math.max(1, (resultCount + PAGE_SIZE - 1) / PAGE_SIZE));
        updatePager();
    }

    private void showSimulator(String query) {
        moduleTitle.setText("Smart bin simulator");
        resultCount = adminService.getBinsCount(null, null, query);
        currentPage = Math.min(currentPage, Math.max(0, (resultCount - 1) / PAGE_SIZE));
        List<Bin> bins = adminService.getBinsPage(currentPage, PAGE_SIZE, null, null, query);
        List<List<String>> rows = bins.stream().map(bin -> List.of(bin.getBinCode(), bin.getZoneId(),
                bin.getWasteType().getDisplayName(), String.format("%.1f%%", bin.getCurrentFillPercentage()),
                bin.getStatus().name())).toList();
        setTable(List.of("Bin", "Zone", "Waste type", "Fill", "Status"), rows);
        resultSummary.setText("Simulator is " + (adminService.getSimulator().isRunning() ? "running" : "stopped")
                + " · displaying " + bins.size() + " of " + resultCount + " bins");
        pageLabel.setText("Page " + (currentPage + 1) + " / " + Math.max(1, (resultCount + PAGE_SIZE - 1) / PAGE_SIZE));
        updatePager();
    }

    private void showReports() {
        moduleTitle.setText("Reports");
        List<String> reportTypes = List.of("Households", "Smart bins", "Complaints", "Points ledger");
        List<List<String>> rows = reportTypes.stream().map(type -> List.of(type, "Export current database records to CSV")).toList();
        setTable(List.of("Report", "Contents"), rows);
        resultCount = rows.size();
        currentPage = 0;
        pageLabel.setText("");
        updatePager();

        Button households = exportButton("Export households", "households");
        Button bins = exportButton("Export bins", "bins");
        Button complaints = exportButton("Export complaints", "complaints");
        Button points = exportButton("Export points ledger", "points");
        moduleActionControls.setVisible(true);
        moduleActionControls.setManaged(true);
        moduleActionControls.getChildren().setAll(households, bins, complaints, points);
        resultSummary.setText("Choose a report. A save dialog will ask where to write the CSV file.");
    }

    private Button exportButton(String label, String reportType) {
        Button button = new Button(label);
        button.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(label);
            chooser.setInitialFileName(reportType + "-report.csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            java.io.File target = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
            if (target == null) return;
            try {
                switch (reportType) {
                    case "households" -> adminService.exportHouseholdsReport(target);
                    case "bins" -> adminService.exportBinsReport(target);
                    case "complaints" -> adminService.exportComplaintsReport(target);
                    case "points" -> adminService.exportPointsLedgerReport(target);
                    default -> throw new IllegalArgumentException("Unknown report type: " + reportType);
                }
                resultSummary.setText("Report exported to " + target.getAbsolutePath());
            } catch (Exception exception) {
                resultSummary.setText(exception.getMessage() == null ? "Report export failed." : exception.getMessage());
            }
        });
        return button;
    }

    private void initializeSimulatorControls() {
        var simulator = adminService.getSimulator();
        Label state = new Label();
        Label speedLabel = new Label("Speed");
        Slider speed = new Slider(0.1, 10.0, simulator.getSpeedMultiplier());
        speed.setShowTickLabels(true);
        speed.setMajorTickUnit(2.0);
        speed.setPrefWidth(200);
        Label speedValue = new Label(String.format("%.1fx", speed.getValue()));
        speed.valueProperty().addListener((observable, oldValue, newValue) -> {
            simulator.setSpeedMultiplier(newValue.doubleValue());
            speedValue.setText(String.format("%.1fx", newValue.doubleValue()));
        });

        Button start = new Button("Start");
        start.setOnAction(event -> {
            simulator.start();
            state.setText("Running");
            if ("simulator".equals(currentModule)) showSimulator(searchField.getText());
        });
        Button stop = new Button("Stop");
        stop.setOnAction(event -> {
            simulator.stop();
            state.setText("Stopped");
            if ("simulator".equals(currentModule)) showSimulator(searchField.getText());
        });
        Button surge = new Button("Festival surge");
        surge.setOnAction(event -> {
            simulator.triggerFestivalSurge();
            resultSummary.setText("Festival surge applied to simulator bins.");
        });
        Button reset = new Button("Reset bin levels");
        reset.setOnAction(event -> {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Reset simulated fill levels to their baseline?", ButtonType.OK, ButtonType.CANCEL);
            confirmation.setHeaderText("Reset simulator data");
            if (confirmation.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
                simulator.resetData();
                resultSummary.setText("Simulator bin levels reset to baseline.");
            }
        });
        state.setText(simulator.isRunning() ? "Running" : "Stopped");
        simulatorControls.getChildren().setAll(state, start, stop, speedLabel, speed, speedValue, surge, reset);
    }

    private void showBinDialog(Bin existing) {
        Dialog<BinForm> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add bin" : "Edit bin");
        dialog.setHeaderText("Smart bin inventory and location");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField id = new TextField(existing == null ? "" : existing.getId());
        TextField code = new TextField(existing == null ? "" : existing.getBinCode());
        ComboBox<Zone> zone = new ComboBox<>(FXCollections.observableArrayList(adminService.getAllZones()));
        zone.setValue(existing == null ? null : adminService.getAllZones().stream()
                .filter(item -> item.getId().equals(existing.getZoneId())).findFirst().orElse(null));
        ComboBox<com.wasteconnect.model.WasteType> type = new ComboBox<>(FXCollections.observableArrayList(com.wasteconnect.model.WasteType.values()));
        type.setValue(existing == null ? com.wasteconnect.model.WasteType.WET : existing.getWasteType());
        TextField capacity = new TextField(existing == null ? "" : Double.toString(existing.getCapacityLiters()));
        TextField fill = new TextField(existing == null ? "" : Double.toString(existing.getCurrentFillPercentage()));
        TextField latitude = new TextField(existing == null ? "" : Double.toString(existing.getLocation().getLatitude()));
        TextField longitude = new TextField(existing == null ? "" : Double.toString(existing.getLocation().getLongitude()));
        if (existing != null) id.setDisable(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.addRow(0, new Label("Bin ID"), id);
        form.addRow(1, new Label("Bin code"), code);
        form.addRow(2, new Label("Zone"), zone);
        form.addRow(3, new Label("Waste type"), type);
        form.addRow(4, new Label("Capacity (L)"), capacity);
        form.addRow(5, new Label("Fill (%)"), fill);
        form.addRow(6, new Label("Latitude"), latitude);
        form.addRow(7, new Label("Longitude"), longitude);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            String validationError = validateBinForm(id.getText(), code.getText(), zone.getValue(), type.getValue(),
                    capacity.getText(), fill.getText(), latitude.getText(), longitude.getText());
            if (validationError != null) {
                event.consume();
                showValidationAlert("Invalid bin details", validationError);
            }
        });
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) return null;
            return new BinForm(id.getText().trim(), code.getText().trim(), zone.getValue().getId(), type.getValue(),
                    Double.parseDouble(capacity.getText().trim()), Double.parseDouble(fill.getText().trim()),
                    Double.parseDouble(latitude.getText().trim()), Double.parseDouble(longitude.getText().trim()));
        });
        dialog.showAndWait().ifPresent(formValues -> {
            try {
                Bin bin = new Bin(formValues.id(), formValues.code(), formValues.zoneId(),
                        new Location(formValues.latitude(), formValues.longitude(), formValues.code()), formValues.type(),
                        formValues.capacity(), formValues.fill(), existing == null ? null : existing.getLastCollectedAt(),
                        existing == null ? BinStatus.NORMAL : existing.getStatus(),
                        existing == null ? 0 : existing.getOpenComplaintsCount());
                if (existing == null) adminService.saveBin(bin);
                else if (!adminService.updateBin(bin)) throw new IllegalStateException("Bin could not be updated.");
                refreshCurrentModule();
                resultSummary.setText(existing == null ? "Bin created." : "Bin updated.");
            } catch (RuntimeException exception) {
                resultSummary.setText(exception.getMessage() == null ? "Bin could not be saved." : exception.getMessage());
            }
        });
    }

    private record BinForm(String id, String code, String zoneId, com.wasteconnect.model.WasteType type,
                           double capacity, double fill, double latitude, double longitude) {}

    private static String validateBinForm(String id, String code, Zone zone, com.wasteconnect.model.WasteType type,
                                          String capacityText, String fillText, String latitudeText, String longitudeText) {
        if (id == null || id.isBlank() || code == null || code.isBlank() || zone == null || type == null) {
            return "Bin ID, code, zone, and waste type are required.";
        }
        try {
            double capacity = Double.parseDouble(capacityText.trim());
            double fill = Double.parseDouble(fillText.trim());
            double latitude = Double.parseDouble(latitudeText.trim());
            double longitude = Double.parseDouble(longitudeText.trim());
            if (capacity < 50) return "Capacity must be at least 50 liters.";
            if (fill < 0 || fill >= 100) return "Fill must be at least 0 and below 100 percent.";
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                return "Latitude must be within -90 to 90 and longitude within -180 to 180.";
            }
        } catch (RuntimeException exception) {
            return "Capacity, fill, latitude, and longitude must be valid numbers.";
        }
        return null;
    }

    private void configureZoneActions() {
        Button addButton = new Button("Add zone");
        addButton.setOnAction(event -> showZoneDialog(null));
        Button editButton = new Button("Edit selected zone");
        editButton.setOnAction(event -> {
            List<String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                resultSummary.setText("Select a zone to edit.");
                return;
            }
            try {
                Zone zone = adminService.getAllZones().stream()
                        .filter(item -> item.getId().equals(selected.get(0))).findFirst().orElseThrow();
                showZoneDialog(zone);
            } catch (RuntimeException exception) {
                resultSummary.setText(exception.getMessage());
            }
        });
        Button deleteButton = new Button("Delete selected zone");
        deleteButton.setOnAction(event -> {
            List<String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                resultSummary.setText("Select a zone to delete.");
                return;
            }
            try {
                if (adminService.deleteZone(selected.get(0))) {
                    refreshCurrentModule();
                    resultSummary.setText("Zone deleted.");
                } else {
                    resultSummary.setText("Zone was not found or could not be deleted.");
                }
            } catch (RuntimeException exception) {
                resultSummary.setText(exception.getMessage() == null ? "Zone could not be deleted." : exception.getMessage());
            }
        });
        moduleActionControls.getChildren().setAll(addButton, editButton, deleteButton);
    }

    private void configureHouseholdActions() {
        Button addButton = new Button("Add household");
        addButton.setOnAction(event -> showHouseholdDialog(null));
        Button editButton = new Button("Edit selected household");
        editButton.setOnAction(event -> {
            List<String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                resultSummary.setText("Select a household to edit.");
                return;
            }
            adminService.getHouseholdsPage(0, Integer.MAX_VALUE, selected.get(0)).stream()
                    .filter(household -> household.getId().equals(selected.get(0)))
                    .findFirst().ifPresentOrElse(this::showHouseholdDialog,
                            () -> resultSummary.setText("Selected household is no longer available."));
        });
        Button deleteButton = new Button("Delete selected household");
        deleteButton.setOnAction(event -> {
            List<String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                resultSummary.setText("Select a household to delete.");
                return;
            }
            try {
                if (adminService.deleteHousehold(selected.get(0))) {
                    refreshCurrentModule();
                    resultSummary.setText("Household deleted.");
                } else {
                    resultSummary.setText("Household was not found or could not be deleted.");
                }
            } catch (RuntimeException exception) {
                resultSummary.setText(exception.getMessage() == null ? "Household could not be deleted." : exception.getMessage());
            }
        });
        moduleActionControls.getChildren().setAll(addButton, editButton, deleteButton);
    }

    private void showHouseholdDialog(Household existing) {
        Dialog<HouseholdForm> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add household" : "Edit household");
        dialog.setHeaderText("Residential location and municipal zone");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField id = new TextField(existing == null ? "" : existing.getId());
        ComboBox<Zone> zone = new ComboBox<>();
        zone.getItems().setAll(adminService.getAllZones());
        zone.setValue(existing == null ? null : adminService.getAllZones().stream()
                .filter(item -> item.getId().equals(existing.getZoneId())).findFirst().orElse(null));
        TextField citizenId = new TextField(existing == null ? "" : valueOrDash(existing.getCitizenId()));
        if ("-".equals(citizenId.getText())) citizenId.clear();
        TextField address = new TextField(existing == null ? "" : existing.getAddress());
        TextField latitude = new TextField(existing == null ? "" : Double.toString(existing.getLocation().getLatitude()));
        TextField longitude = new TextField(existing == null ? "" : Double.toString(existing.getLocation().getLongitude()));
        if (existing != null) id.setDisable(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.addRow(0, new Label("Household ID"), id);
        form.addRow(1, new Label("Zone"), zone);
        form.addRow(2, new Label("Citizen ID (optional)"), citizenId);
        form.addRow(3, new Label("Address"), address);
        form.addRow(4, new Label("Latitude"), latitude);
        form.addRow(5, new Label("Longitude"), longitude);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            String validationError = validateHouseholdForm(id.getText(), zone.getValue(), address.getText(),
                    latitude.getText(), longitude.getText());
            if (validationError != null) {
                event.consume();
                showValidationAlert("Invalid household details", validationError);
            }
        });
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) return null;
            return new HouseholdForm(id.getText().trim(), zone.getValue().getId(), nullableValue(citizenId.getText()),
                    address.getText().trim(), Double.parseDouble(latitude.getText().trim()),
                    Double.parseDouble(longitude.getText().trim()));
        });
        dialog.showAndWait().ifPresent(formValues -> {
            try {
                Household household = new Household(formValues.id(), formValues.zoneId(), formValues.citizenId(),
                        formValues.address(), new Location(formValues.latitude(), formValues.longitude(), formValues.address()));
                if (existing == null) {
                    adminService.saveHousehold(household);
                } else if (!adminService.updateHousehold(household)) {
                    throw new IllegalStateException("Household could not be updated.");
                }
                currentPage = 0;
                refreshCurrentModule();
                resultSummary.setText(existing == null ? "Household created." : "Household updated.");
            } catch (RuntimeException exception) {
                resultSummary.setText(exception.getMessage() == null ? "Household could not be saved." : exception.getMessage());
            }
        });
    }

    private record HouseholdForm(String id, String zoneId, String citizenId, String address,
                                 double latitude, double longitude) {}

    private static String validateHouseholdForm(String id, Zone zone, String address, String latitudeText, String longitudeText) {
        if (id == null || id.isBlank() || zone == null || address == null || address.isBlank()) {
            return "Household ID, zone, and address are required.";
        }
        try {
            double latitude = Double.parseDouble(latitudeText.trim());
            double longitude = Double.parseDouble(longitudeText.trim());
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                return "Latitude must be within -90 to 90 and longitude within -180 to 180.";
            }
        } catch (RuntimeException exception) {
            return "Enter valid numeric latitude and longitude values.";
        }
        return null;
    }

    private static String nullableValue(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void configureVehicleActions() {
        Button addButton = new Button("Add vehicle");
        addButton.setOnAction(event -> showVehicleDialog(null));
        Button editButton = new Button("Edit selected vehicle");
        editButton.setOnAction(event -> {
            List<String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                resultSummary.setText("Select a vehicle to edit.");
                return;
            }
            adminService.getAllVehicles().stream().filter(vehicle -> vehicle.getId().equals(selected.get(0)))
                    .findFirst().ifPresentOrElse(this::showVehicleDialog,
                            () -> resultSummary.setText("Selected vehicle is no longer available."));
        });
        Button deleteButton = new Button("Delete selected vehicle");
        deleteButton.setOnAction(event -> {
            List<String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                resultSummary.setText("Select a vehicle to delete.");
                return;
            }
            try {
                if (adminService.deleteVehicle(selected.get(0))) {
                    refreshCurrentModule();
                    resultSummary.setText("Vehicle deleted.");
                } else {
                    resultSummary.setText("Vehicle was not found or could not be deleted.");
                }
            } catch (RuntimeException exception) {
                resultSummary.setText(exception.getMessage() == null ? "Vehicle could not be deleted." : exception.getMessage());
            }
        });
        moduleActionControls.getChildren().setAll(addButton, editButton, deleteButton);
    }

    private void configureCollectorActions() {
        Button add = new Button("Add collector");
        add.setOnAction(event -> showCollectorDialog(null));
        Button edit = new Button("Edit selected collector");
        edit.setOnAction(event -> {
            List<String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                resultSummary.setText("Select a collector to edit.");
                return;
            }
            adminService.getAllCollectors().stream().filter(collector -> collector.getId().equals(selected.get(0)))
                    .findFirst().ifPresentOrElse(this::showCollectorDialog,
                            () -> resultSummary.setText("Selected collector was not found."));
        });
        Button delete = new Button("Delete selected collector");
        delete.setOnAction(event -> {
            List<String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                resultSummary.setText("Select a collector to delete.");
                return;
            }
            try {
                if (adminService.deleteCollector(selected.get(0))) {
                    refreshCurrentModule();
                    resultSummary.setText("Collector account deleted.");
                } else {
                    resultSummary.setText("Collector account was not found.");
                }
            } catch (RuntimeException exception) {
                resultSummary.setText(exception.getMessage() == null ? "Collector could not be deleted." : exception.getMessage());
            }
        });
        moduleActionControls.getChildren().setAll(add, edit, delete);
    }

    private void showCollectorDialog(Collector existing) {
        boolean creating = existing == null;
        Dialog<CollectorForm> dialog = new Dialog<>();
        dialog.setTitle(creating ? "Create collector account" : "Edit collector");
        dialog.setHeaderText(creating ? "Create a secure login and assign a zone and vehicle" : "Update collector details and assignments");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField name = new TextField(creating ? "" : existing.getName());
        TextField email = new TextField(creating ? "" : existing.getEmail());
        PasswordField password = new PasswordField();
        password.setPromptText(creating ? "Temporary password" : "Leave blank to keep current password");
        TextField employeeId = new TextField(creating ? "" : existing.getEmployeeId());
        ComboBox<Zone> zone = new ComboBox<>(FXCollections.observableArrayList(adminService.getAllZones()));
        zone.setValue(creating ? null : adminService.getAllZones().stream()
                .filter(item -> item.getId().equals(existing.getAssignedZoneId())).findFirst().orElse(null));
        ComboBox<Vehicle> vehicle = new ComboBox<>(FXCollections.observableArrayList(adminService.getAllVehicles()));
        vehicle.setValue(creating ? null : adminService.getAllVehicles().stream()
                .filter(item -> item.getId().equals(existing.getAssignedVehicleId())).findFirst().orElse(null));
        CheckBox active = new CheckBox("Account active");
        active.setSelected(creating || existing.isActive());

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.addRow(0, new Label("Full name"), name);
        form.addRow(1, new Label("Email"), email);
        if (creating) form.addRow(2, new Label("Password"), password);
        form.addRow(3, new Label("Employee ID"), employeeId);
        form.addRow(4, new Label("Assigned zone"), zone);
        form.addRow(5, new Label("Assigned vehicle"), vehicle);
        form.add(active, 1, 6);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            String validationError = validateCollectorForm(name.getText(), email.getText(),
                    creating ? password.getText() : "valid", employeeId.getText(), zone.getValue(), vehicle.getValue());
            if (validationError != null) {
                event.consume();
                showValidationAlert("Invalid collector details", validationError);
            }
        });
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) return null;
            return new CollectorForm(name.getText().trim(), email.getText().trim(), password.getText(),
                    employeeId.getText().trim(), zone.getValue(), vehicle.getValue(), active.isSelected());
        });
        dialog.showAndWait().ifPresent(values -> {
            try {
                if (creating) {
                    Collector saved = adminService.saveCollector(values.name(), values.email(), values.password(),
                            values.employeeId(), values.zone().getId(), values.vehicle().getId());
                    if (!values.active()) {
                        saved.setActive(false);
                        adminService.updateCollector(saved);
                    }
                    resultSummary.setText("Collector account created: " + saved.getId());
                } else {
                    Collector updated = new Collector(existing.getId(), values.name(), values.email(), existing.getPasswordHash(),
                            existing.getCreatedAt(), values.active(), values.employeeId(), values.zone().getId(),
                            values.vehicle().getId(), existing.getRating(), existing.getTotalPickupsCompleted());
                    if (!adminService.updateCollector(updated)) throw new IllegalStateException("Collector could not be updated.");
                    resultSummary.setText("Collector updated.");
                }
                refreshCurrentModule();
            } catch (RuntimeException exception) {
                resultSummary.setText(exception.getMessage() == null ? "Collector could not be saved." : exception.getMessage());
            }
        });
    }

    private record CollectorForm(String name, String email, String password, String employeeId,
                                 Zone zone, Vehicle vehicle, boolean active) {}

    private static String validateCollectorForm(String name, String email, String password,
                                                String employeeId, Zone zone, Vehicle vehicle) {
        if (name == null || name.trim().length() < 2 || employeeId == null || employeeId.isBlank()
                || zone == null || vehicle == null) {
            return "Name, employee ID, zone, and vehicle are required.";
        }
        if (!com.wasteconnect.util.Validators.isValidEmail(email)) return "Enter a valid email address.";
        if (password == null || password.length() < 6) return "Password must be at least 6 characters.";
        return null;
    }

    private void showVehicleDialog(Vehicle existing) {
        Dialog<VehicleForm> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add vehicle" : "Edit vehicle");
        dialog.setHeaderText("Municipal fleet details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField id = new TextField(existing == null ? "" : existing.getId());
        TextField registration = new TextField(existing == null ? "" : existing.getRegistrationNumber());
        TextField capacity = new TextField(existing == null ? "" : Double.toString(existing.getCapacityKg()));
        ComboBox<VehicleType> type = new ComboBox<>();
        type.getItems().setAll(VehicleType.values());
        type.setValue(existing == null ? VehicleType.SMALL_VEHICLE : existing.getVehicleType());
        CheckBox active = new CheckBox("Active");
        active.setSelected(existing == null || existing.isActive());
        if (existing != null) id.setDisable(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.addRow(0, new Label("Vehicle ID"), id);
        form.addRow(1, new Label("Registration"), registration);
        form.addRow(2, new Label("Type"), type);
        form.addRow(3, new Label("Capacity (kg)"), capacity);
        form.add(active, 1, 4);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            String validationError = validateVehicleForm(id.getText(), registration.getText(), type.getValue(), capacity.getText());
            if (validationError != null) {
                event.consume();
                showValidationAlert("Invalid vehicle details", validationError);
            }
        });
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) return null;
            return new VehicleForm(id.getText().trim(), registration.getText().trim(), type.getValue(),
                    Double.parseDouble(capacity.getText().trim()), active.isSelected());
        });

        dialog.showAndWait().ifPresent(formValues -> {
            try {
                Vehicle vehicle = formValues.type() == VehicleType.SMALL_VEHICLE
                        ? new SmallVehicle(formValues.id(), formValues.registration(), formValues.capacity())
                        : new LargeTruck(formValues.id(), formValues.registration(), formValues.capacity());
                vehicle.setActive(formValues.active());
                if (existing == null) {
                    adminService.saveVehicle(vehicle);
                } else if (!adminService.updateVehicle(vehicle)) {
                    throw new IllegalStateException("Vehicle could not be updated.");
                }
                refreshCurrentModule();
                resultSummary.setText(existing == null ? "Vehicle created." : "Vehicle updated.");
            } catch (RuntimeException exception) {
                resultSummary.setText(exception.getMessage() == null ? "Vehicle could not be saved." : exception.getMessage());
            }
        });
    }

    private record VehicleForm(String id, String registration, VehicleType type, double capacity, boolean active) {}

    private void showZoneDialog(Zone existing) {
        Dialog<Zone> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add zone" : "Edit zone");
        dialog.setHeaderText("Municipal service area details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField id = new TextField(existing == null ? "" : existing.getId());
        TextField name = new TextField(existing == null ? "" : existing.getName());
        TextField code = new TextField(existing == null ? "" : existing.getCode());
        TextField latitude = new TextField(existing == null ? "" : Double.toString(existing.getCenterLat()));
        TextField longitude = new TextField(existing == null ? "" : Double.toString(existing.getCenterLng()));
        CheckBox market = new CheckBox("Commercial market area");
        market.setSelected(existing != null && existing.isMarketArea());
        if (existing != null) id.setDisable(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.addRow(0, new Label("Zone ID"), id);
        form.addRow(1, new Label("Name"), name);
        form.addRow(2, new Label("Code"), code);
        form.addRow(3, new Label("Center latitude"), latitude);
        form.addRow(4, new Label("Center longitude"), longitude);
        form.add(market, 1, 5);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            String validationError = validateZoneForm(id.getText(), name.getText(), code.getText(),
                    latitude.getText(), longitude.getText());
            if (validationError != null) {
                event.consume();
                showValidationAlert("Invalid zone details", validationError);
            }
        });
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) return null;
            return new Zone(id.getText().trim(), name.getText().trim(), code.getText().trim(),
                    Double.parseDouble(latitude.getText().trim()), Double.parseDouble(longitude.getText().trim()), market.isSelected());
        });

        dialog.showAndWait().ifPresent(zone -> {
            try {
                if (existing == null) {
                    adminService.saveZone(zone);
                } else if (!adminService.updateZone(zone)) {
                    throw new IllegalStateException("Zone could not be updated.");
                }
                refreshCurrentModule();
                resultSummary.setText(existing == null ? "Zone created." : "Zone updated.");
            } catch (RuntimeException exception) {
                resultSummary.setText(exception.getMessage() == null ? "Zone could not be saved." : exception.getMessage());
            }
        });
    }

    private static String validateVehicleForm(String id, String registration, VehicleType type, String capacityText) {
        if (id == null || id.isBlank() || registration == null || registration.isBlank() || type == null) {
            return "Vehicle ID, registration, and type are required.";
        }
        try {
            if (Double.parseDouble(capacityText.trim()) <= 0) return "Capacity must be greater than zero.";
        } catch (RuntimeException exception) {
            return "Capacity must be a valid number greater than zero.";
        }
        return null;
    }

    private static String validateZoneForm(String id, String name, String code, String latitudeText, String longitudeText) {
        if (id == null || id.isBlank() || name == null || name.isBlank() || code == null || code.isBlank()) {
            return "Zone ID, name, and code are required.";
        }
        try {
            double latitude = Double.parseDouble(latitudeText.trim());
            double longitude = Double.parseDouble(longitudeText.trim());
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                return "Latitude must be within -90 to 90 and longitude within -180 to 180.";
            }
        } catch (RuntimeException exception) {
            return "Enter valid numeric latitude and longitude values.";
        }
        return null;
    }

    private static void showValidationAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    private void configurePickerActions() {
        Button approveButton = new Button("Approve selected picker");
        Button rejectButton = new Button("Reject selected picker");
        approveButton.setOnAction(event -> updateSelectedPicker(true));
        rejectButton.setOnAction(event -> updateSelectedPicker(false));
        moduleActionControls.getChildren().setAll(approveButton, rejectButton);
    }

    private void updateSelectedPicker(boolean approved) {
        List<String> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            resultSummary.setText("Select a waste picker first.");
            return;
        }
        try {
            if (!adminService.verifyWastePicker(selected.get(0), approved)) {
                resultSummary.setText("Picker verification status was not changed.");
                return;
            }
            refreshCurrentModule();
            resultSummary.setText("Picker " + selected.get(0) + (approved ? " approved." : " rejected."));
        } catch (RuntimeException exception) {
            resultSummary.setText(exception.getMessage() == null ? "Verification update failed." : exception.getMessage());
        }
    }

    private void showRoutes(String query) {
        moduleTitle.setText("Route planning");
        Zone zone = routeZoneSelector.getValue();
        if (zone == null) {
            setTable(List.of("Route", "Date", "Strategy", "Stop", "Stop ID", "Status"), List.of());
            resultSummary.setText("No zones are available for route planning.");
            resultCount = 0;
            updatePager();
            return;
        }
        List<Route> routes = adminService.getRoutesForZone(zone.getId(), LocalDate.now());
        List<List<String>> rows = routes.stream().flatMap(route -> {
            if (route.getStops().isEmpty()) {
                return java.util.stream.Stream.of(List.of(route.getId(), route.getRouteDate().toString(),
                        route.getStrategyUsed(), "-", "No stops", route.getStatus()));
            }
            return route.getStops().stream().map(stop -> List.of(route.getId(), route.getRouteDate().toString(),
                    route.getStrategyUsed(), Integer.toString(stop.getSequence()), stop.getStopId(),
                    stop.isCollected() ? "Collected" : "Pending"));
        }).filter(row -> query == null || query.isBlank()
            || row.stream().anyMatch(value -> value.toLowerCase().contains(query.toLowerCase()))).toList();
        resultCount = rows.size();
        currentPage = Math.min(currentPage, Math.max(0, (resultCount - 1) / PAGE_SIZE));
        int fromIndex = Math.min(currentPage * PAGE_SIZE, resultCount);
        int toIndex = Math.min(fromIndex + PAGE_SIZE, resultCount);
        setTable(List.of("Route", "Date", "Strategy", "Stop", "Stop ID", "Status"), rows.subList(fromIndex, toIndex));
        pageLabel.setText("Page " + (currentPage + 1) + " / " + Math.max(1, (resultCount + PAGE_SIZE - 1) / PAGE_SIZE));
        resultSummary.setText("Showing " + (resultCount == 0 ? 0 : fromIndex + 1) + "-" + toIndex + " of "
            + resultCount + " route stops across " + routes.size() + " routes for " + zone.getName() + " today");
        updatePager();
    }

    private void setTable(List<String> headers, List<List<String>> rows) {
        table.getColumns().clear();
        for (int index = 0; index < headers.size(); index++) {
            final int columnIndex = index;
            String header = headers.get(index);
            TableColumn<List<String>, String> column = new TableColumn<>(header);
            column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().get(columnIndex)));
            column.setStyle("-fx-alignment: CENTER-LEFT;");
            setColumnWidth(column, header);
            table.getColumns().add(column);
        }
        table.setItems(FXCollections.observableArrayList(new ArrayList<>(rows)));
        Platform.runLater(() -> resizeFinalColumn(table.getWidth()));
    }

    private void resizeFinalColumn(double tableWidth) {
        if (table.getColumns().isEmpty() || tableWidth <= 0) {
            return;
        }
        double fixedWidth = table.getColumns().stream()
                .limit(table.getColumns().size() - 1L)
                .mapToDouble(TableColumn::getWidth)
                .sum();
        TableColumn<List<String>, ?> finalColumn = table.getColumns().get(table.getColumns().size() - 1);
        double minimumWidth = finalColumn.getMinWidth();
        finalColumn.setPrefWidth(Math.max(minimumWidth, tableWidth - fixedWidth - 24));
    }

    private void updatePager() {
        previousButton.setDisable(currentPage <= 0);
        nextButton.setDisable((currentPage + 1) * PAGE_SIZE >= resultCount);
    }

    private void subscribeToEvents() {
        EventBus bus = EventBus.getInstance();
        eventSubscriptions.add(bus.subscribe(EventBus.HouseholdCollected.class, event -> refreshFromEvent()));
        eventSubscriptions.add(bus.subscribe(EventBus.ComplaintRaised.class, event -> refreshFromEvent()));
        eventSubscriptions.add(bus.subscribe(EventBus.ComplaintAssigned.class, event -> refreshFromEvent()));
        eventSubscriptions.add(bus.subscribe(EventBus.ComplaintResolved.class, event -> refreshFromEvent()));
        eventSubscriptions.add(bus.subscribe(EventBus.ComplaintEscalated.class, event -> refreshFromEvent()));
        eventSubscriptions.add(bus.subscribe(EventBus.RouteGenerated.class, event -> refreshFromEvent()));
        eventSubscriptions.add(bus.subscribe(EventBus.RouteReplanned.class, event -> refreshFromEvent()));
        eventSubscriptions.add(bus.subscribe(EventBus.PickerVerified.class, event -> refreshFromEvent()));
        eventSubscriptions.add(bus.subscribe(EventBus.BinOverflow.class, event -> refreshFromEvent()));
        eventSubscriptions.add(bus.subscribe(EventBus.BinFillChanged.class, event -> {
            if ("simulator".equals(currentModule)) refreshFromEvent();
        }));
        eventSubscriptions.add(bus.subscribe(EventBus.NotificationCreated.class, event -> {
            if (adminUser.getId().equals(event.userId())) Platform.runLater(this::refreshNotificationCount);
        }));
    }

    private void refreshNotificationCount() {
        notificationsButton.setText("Notifications (" + notificationService.getUnreadCount(adminUser.getId()) + ")");
    }

    private void showNotificationsDialog() {
        TableView<Notification> notificationTable = new TableView<>();
        notificationTable.getColumns().addAll(
                notificationColumn("Received", notification -> notification.getCreatedAt().toString().replace('T', ' ')),
                notificationColumn("Read", notification -> notification.isRead() ? "Read" : "Unread"),
                notificationColumn("Title", Notification::getTitle),
                notificationColumn("Message", Notification::getMessage));
        notificationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        notificationTable.setItems(FXCollections.observableArrayList(notificationService.getUserNotifications(adminUser.getId())));
        notificationTable.setPlaceholder(new Label("No notifications yet."));

        Button markSelected = new Button("Mark selected read");
        markSelected.setOnAction(event -> {
            Notification selected = notificationTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            notificationService.markAsRead(selected.getId());
            notificationTable.setItems(FXCollections.observableArrayList(notificationService.getUserNotifications(adminUser.getId())));
            refreshNotificationCount();
        });
        Button markAll = new Button("Mark all read");
        markAll.setOnAction(event -> {
            notificationService.markAllAsRead(adminUser.getId());
            notificationTable.setItems(FXCollections.observableArrayList(notificationService.getUserNotifications(adminUser.getId())));
            refreshNotificationCount();
        });
        VBox content = new VBox(10, new HBox(10, markSelected, markAll), notificationTable);
        content.setPrefSize(760, 420);
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Admin notifications");
        dialog.setHeaderText(notificationsButton.getText());
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private static TableColumn<Notification, String> notificationColumn(String title,
                                                                          java.util.function.Function<Notification, String> value) {
        TableColumn<Notification, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private void refreshFromEvent() {
        if (eventRefreshQueued.compareAndSet(false, true)) {
            Platform.runLater(() -> {
                try {
                    refreshCurrentModule();
                } finally {
                    eventRefreshQueued.set(false);
                }
            });
        }
    }

    private void unsubscribeFromEvents() {
        for (AutoCloseable subscription : eventSubscriptions) {
            try {
                subscription.close();
            } catch (Exception ignored) {
                // Subscriptions are best-effort cleanup during scene replacement.
            }
        }
        eventSubscriptions.clear();
    }

    private static void setColumnWidth(TableColumn<List<String>, String> column, String header) {
        double preferredWidth = switch (header) {
            case "Household ID", "Complaint ID", "Complaint", "Vehicle ID", "Picker ID" -> 150;
            case "Zone", "Code", "Status", "Type", "Fill", "Active", "Verified", "Records" -> 120;
            case "Citizen", "Name", "Capacity", "Market area", "Collected", "Value" -> 150;
            case "Address", "Description", "Assigned collector", "Coverage", "Recommendation" -> 360;
            case "Location", "Advantage" -> 220;
            case "Risk Level", "Score", "Overflows", "Complaints" -> 130;
            case "Email", "Registration" -> 230;
            default -> 170;
        };
        column.setMinWidth(Math.min(preferredWidth, 120));
        column.setPrefWidth(preferredWidth);
        column.setResizable(false);
    }

    private static String coordinates(double latitude, double longitude) {
        return String.format("%.4f, %.4f", latitude, longitude);
    }

    private static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}