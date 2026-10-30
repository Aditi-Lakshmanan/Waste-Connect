package com.wasteconnect.ui;

import com.wasteconnect.exception.ValidationException;
import com.wasteconnect.model.Citizen;
import com.wasteconnect.model.Complaint;
import com.wasteconnect.model.ComplaintEvent;
import com.wasteconnect.model.LeaderboardEntry;
import com.wasteconnect.model.ComplaintType;
import com.wasteconnect.model.ListingStatus;
import com.wasteconnect.model.Location;
import com.wasteconnect.model.Notification;
import com.wasteconnect.model.PickupRequest;
import com.wasteconnect.model.RecyclableListing;
import com.wasteconnect.model.RewardTransaction;
import com.wasteconnect.model.Route;
import com.wasteconnect.model.RouteStop;
import com.wasteconnect.model.SegregationQuality;
import com.wasteconnect.model.User;
import com.wasteconnect.model.UserRole;
import com.wasteconnect.model.WastePicker;
import com.wasteconnect.model.WasteType;
import com.wasteconnect.service.CitizenService;
import com.wasteconnect.service.CollectorService;
import com.wasteconnect.service.WastePickerService;
import com.wasteconnect.service.NotificationService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import com.wasteconnect.pattern.EventBus;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class RoleWorkspaceView extends BorderPane {
    private final SceneManager sceneManager;
    private final User user;
    private final TabPane tabs = new TabPane();
    private final CitizenService citizenService = new CitizenService();
    private final CollectorService collectorService = new CollectorService();
    private final WastePickerService pickerService = new WastePickerService();
    private final NotificationService notificationService = new NotificationService();
    private final Label status = new Label("Ready");
    private final Label notificationCountLabel = new Label();
    private final List<AutoCloseable> eventSubscriptions = new ArrayList<>();

    private TableView<PickupRequest> pickupsTable;
    private TableView<RewardTransaction> rewardsTable;
    private TableView<Complaint> citizenComplaintsTable;
    private TableView<RecyclableListing> citizenListingsTable;
    private TableView<RouteStop> routeTable;
    private TableView<Complaint> assignedComplaintsTable;
    private TableView<RecyclableListing> availableListingsTable;
    private TableView<RecyclableListing> claimedListingsTable;
    private TableView<Notification> notificationsTable;
    private Label nextPickupLabel;
    private Label pointsLabel;
    private Label pickerStatusLabel;
    private Label pickerEarningsLabel;
    private TextField pickerLatitudeField;
    private TextField pickerLongitudeField;

    public RoleWorkspaceView(SceneManager sceneManager, User user) {
        this.sceneManager = sceneManager;
        this.user = user;
        setTop(buildHeader());
        setCenter(tabs);
        setBottom(buildStatusBar());
        switch (user.getRole()) {
            case CITIZEN -> buildCitizenWorkspace();
            case COLLECTOR -> buildCollectorWorkspace();
            case WASTE_PICKER -> buildPickerWorkspace();
            default -> throw new IllegalArgumentException("Role workspace is only for non-admin operational roles.");
        }
        buildNotificationsTab();
        subscribeToEvents();
        sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) unsubscribeFromEvents();
        });
    }

    private Node buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setStyle("-fx-background-color: #19241E; -fx-border-color: transparent transparent #29382F transparent;");

        VBox identity = new VBox(3);
        Label title = new Label("WasteConnect");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #83D59D;");
        Label account = new Label(user.getName() + "  /  " + displayRole(user.getRole()));
        account.setStyle("-fx-text-fill: #A2B1A7;");
        identity.getChildren().addAll(title, account);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button refresh = new Button("Refresh data");
        refresh.setOnAction(event -> refreshAll());
        Button logout = new Button("Logout");
        logout.setOnAction(event -> sceneManager.showLogout());
        header.getChildren().addAll(identity, spacer, refresh, logout);
        return header;
    }

    private Node buildStatusBar() {
        HBox footer = new HBox(status);
        footer.setPadding(new Insets(9, 24, 10, 24));
        footer.setStyle("-fx-border-color: #29382F transparent transparent transparent;");
        status.setStyle("-fx-text-fill: #A2B1A7;");
        return footer;
    }

    private void buildCitizenWorkspace() {
        Citizen citizen = citizenService.getCitizen(user.getId()).orElse(null);
        nextPickupLabel = new Label();
        pointsLabel = new Label();
        nextPickupLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #E5EEE8;");
        pointsLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #83D59D;");

        VBox overview = new VBox(14,
                heading("Household dashboard", "Manage collection requests, recyclable handoffs, complaints, and rewards."),
                infoPanel("Next scheduled collection", nextPickupLabel),
                infoPanel("Available reward points", pointsLabel));
        overview.setPadding(new Insets(24));
        tabs.getTabs().add(tab("Overview", overview));

        pickupsTable = table(
                column("Request", PickupRequest::getId),
                column("Waste type", request -> request.getWasteType().getDisplayName()),
                column("Weight", request -> String.format("%.1f kg", request.getApproximateWeightKg())),
                column("Preferred date", request -> request.getPreferredSlot().toString()),
                column("Status", request -> request.getStatus().name()),
                column("Address", PickupRequest::getAddress));
        DatePicker pickupDate = new DatePicker();
        pickupDate.setPromptText("Select collection date");
        ComboBox<WasteType> pickupType = combo(WasteType.values());
        TextField pickupWeight = field("Estimated kg", "");
        TextField pickupAddress = field("Pickup address", citizen == null ? "" : citizen.getAddress());
        Button requestPickup = new Button("Request collection");
        requestPickup.setOnAction(event -> runAction(() -> {
            citizenService.requestPickup(user.getId(), pickupType.getValue(), parseDouble(pickupWeight, "Weight"),
                    pickupDate.getValue(), pickupAddress.getText());
            refreshCitizenData();
            status.setText("Collection request submitted.");
        }));
        VBox pickupView = new VBox(12, heading("Collection schedule", "Choose the waste stream, estimated amount, address, and preferred date."),
                formRow(pickupType, pickupWeight, pickupDate, pickupAddress, requestPickup), pickupsTable);
        pickupView.setPadding(new Insets(20));
        VBox.setVgrow(pickupsTable, Priority.ALWAYS);
        tabs.getTabs().add(tab("Collections", pickupView));

        rewardsTable = table(
                column("Date", transaction -> transaction.getTimestamp().toLocalDate().toString()),
                column("Activity", transaction -> transaction.getType().name()),
                column("Points", transaction -> String.format("%+d", transaction.getPoints())),
                column("Details", RewardTransaction::getDescription));
        TextField redeemAmount = field("Points to redeem", "");
        TextField rewardName = field("Reward", "");
        Button redeem = new Button("Redeem points");
        redeem.setOnAction(event -> runAction(() -> {
            citizenService.redeemPoints(user.getId(), parseInt(redeemAmount, "Points"), rewardName.getText());
            refreshCitizenData();
            String description = citizenService.getRewardTransactions(user.getId()).stream()
                    .filter(transaction -> transaction.getType() == RewardTransaction.Type.REDEEMED)
                    .findFirst().map(RewardTransaction::getDescription).orElse("Redemption recorded.");
            status.setText(description.contains("Code:") ? description.substring(description.indexOf("Code:")) : description);
        }));
        VBox rewards = new VBox(12, heading("Rewards wallet", "Points are earned or deducted when a collector records your segregation quality."),
                pointsLabel, formRow(redeemAmount, rewardName, redeem), rewardsTable);
        rewards.setPadding(new Insets(20));
        VBox.setVgrow(rewardsTable, Priority.ALWAYS);
        tabs.getTabs().add(tab("Points", rewards));

        citizenComplaintsTable = table(
                column("Complaint", Complaint::getId),
                column("Type", complaint -> complaint.getComplaintType().getLabel()),
                column("Status", complaint -> complaint.getStatus().name()),
                column("Description", Complaint::getDescription));
        ComboBox<ComplaintType> complaintType = combo(ComplaintType.values());
        TextField complaintDescription = field("Describe the issue", "");
        TextField complaintLatitude = field("Latitude", "");
        TextField complaintLongitude = field("Longitude", "");
        Button reportIssue = new Button("Submit complaint");
        reportIssue.setOnAction(event -> runAction(() -> {
            citizenService.raiseComplaint(user.getId(), complaintType.getValue(), complaintDescription.getText(),
                parseDouble(complaintLatitude, "Latitude"), parseDouble(complaintLongitude, "Longitude"),
                citizen == null ? "Z1" : citizen.getZoneId());
            complaintDescription.clear();
            refreshCitizenData();
            status.setText("Complaint submitted.");
        }));
        VBox complaints = new VBox(12, heading("Report a collection issue", "Your report is tracked with the service-level target for its category."),
            formRow(complaintType, complaintDescription, complaintLatitude, complaintLongitude, reportIssue), citizenComplaintsTable);
        Button viewTimeline = new Button("View selected complaint timeline");
        viewTimeline.setOnAction(event -> runAction(() -> {
            Complaint complaint = requireSelection(citizenComplaintsTable, "Select a complaint to view its timeline.");
            showComplaintTimeline(complaint);
        }));
        complaints.getChildren().add(3, viewTimeline);
        complaints.setPadding(new Insets(20));
        VBox.setVgrow(citizenComplaintsTable, Priority.ALWAYS);
        tabs.getTabs().add(tab("Complaints", complaints));

        citizenListingsTable = table(
                column("Listing", RecyclableListing::getId),
                column("Material", listing -> listing.getWasteType().getDisplayName()),
                column("Weight", listing -> String.format("%.1f kg", listing.getWeightKg())),
                column("Estimated value", listing -> String.format("₹%.0f", listing.getEstimatedValue())),
                column("Status", listing -> listing.getStatus().name()));
        ComboBox<WasteType> recyclableType = combo(new WasteType[]{WasteType.DRY, WasteType.E_WASTE});
        TextField recyclableWeight = field("Estimated kg", "");
        TextField listingAddress = field("Pickup address", citizen == null ? "" : citizen.getAddress());
        TextField listingLatitude = field("Latitude", "");
        TextField listingLongitude = field("Longitude", "");
        Button postListing = new Button("Offer recyclables");
        postListing.setOnAction(event -> runAction(() -> {
            citizenService.listRecyclables(user.getId(), recyclableType.getValue(), parseDouble(recyclableWeight, "Weight"),
                listingAddress.getText(), parseDouble(listingLatitude, "Latitude"), parseDouble(listingLongitude, "Longitude"));
            refreshCitizenData();
            status.setText("Recyclable listing is now available to verified pickers.");
        }));
        VBox listings = new VBox(12, heading("Recyclable pickup", "Post dry recyclables or e-waste for a verified picker to claim."),
            formRow(recyclableType, recyclableWeight, listingAddress, listingLatitude, listingLongitude, postListing), citizenListingsTable);
        listings.setPadding(new Insets(20));
        VBox.setVgrow(citizenListingsTable, Priority.ALWAYS);
        tabs.getTabs().add(tab("Recyclables", listings));

        TextField guideSearch = field("Search materials or instructions", "");
        TableView<CitizenService.SegregationGuideItem> guideTable = table(
            column("Material", CitizenService.SegregationGuideItem::name),
            column("Category", CitizenService.SegregationGuideItem::category),
            column("Waste stream", item -> item.binType().getDisplayName()),
            column("Instructions", CitizenService.SegregationGuideItem::instructions));
        List<CitizenService.SegregationGuideItem> guideItems = CitizenService.getSegregationGuide();
        Runnable filterGuide = () -> {
            String query = guideSearch.getText() == null ? "" : guideSearch.getText().trim().toLowerCase();
            guideTable.setItems(FXCollections.observableArrayList(guideItems.stream().filter(item -> query.isEmpty()
                || item.name().toLowerCase().contains(query)
                || item.category().toLowerCase().contains(query)
                || item.instructions().toLowerCase().contains(query)
                || item.binType().getDisplayName().toLowerCase().contains(query)).toList()));
        };
        guideSearch.textProperty().addListener((observable, oldValue, newValue) -> filterGuide.run());
        filterGuide.run();
        VBox guide = new VBox(12, heading("Segregation guide", "Search disposal guidance by material, category, or waste stream."), guideSearch, guideTable);
        guide.setPadding(new Insets(20));
        VBox.setVgrow(guideTable, Priority.ALWAYS);
        tabs.getTabs().add(tab("Guide", guide));

        TableView<LeaderboardEntry> leaderboardTable = table(
            column("Rank", entry -> Integer.toString(entry.rank())),
            column("Citizen", LeaderboardEntry::citizenName),
            column("Zone", LeaderboardEntry::zoneName),
            column("Points", entry -> Integer.toString(entry.points())),
            column("Score", entry -> String.format("%.1f", entry.segregationScore())),
            column("Streak", entry -> Integer.toString(entry.streak())));
        leaderboardTable.setItems(FXCollections.observableArrayList(citizenService.getLeaderboard(20)));
        VBox leaderboard = new VBox(12, heading("Zone leaderboard", "Current standings from stored household points and segregation scores."), leaderboardTable);
        leaderboard.setPadding(new Insets(20));
        VBox.setVgrow(leaderboardTable, Priority.ALWAYS);
        tabs.getTabs().add(tab("Leaderboard", leaderboard));
        refreshCitizenData();
    }

        private void showComplaintTimeline(Complaint complaint) {
        TableView<ComplaintEvent> timelineTable = table(
            column("Time", event -> event.getTimestamp().toString().replace('T', ' ')),
            column("From", event -> event.getFromStatus() == null ? "Submitted" : event.getFromStatus().getDisplayName()),
            column("To", event -> event.getToStatus().getDisplayName()),
            column("Notes", ComplaintEvent::getRemarks));
        timelineTable.setItems(FXCollections.observableArrayList(citizenService.getComplaintTimeline(complaint.getId())));
        javafx.scene.control.Alert dialog = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        dialog.setTitle("Complaint timeline");
        dialog.setHeaderText(complaint.getId() + " · " + complaint.getStatus().getDisplayName());
        VBox content = new VBox(10, new Label(complaint.getDescription()), timelineTable);
        content.setPrefSize(760, 380);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
        }

    private void buildCollectorWorkspace() {
        routeTable = table(
                column("Stop", stop -> Integer.toString(stop.getSequence())),
                column("Location / bin", RouteStop::getStopId),
                column("Demand", stop -> String.format("%.1f kg", stop.getDemandKg())),
                column("Priority", stop -> String.format("%.1f", stop.getPriorityScore())),
                column("Notes", RouteStop::getNotes));
        routeTable.setPlaceholder(new Label("No route is assigned for today. Contact dispatch for an assignment."));
        ComboBox<SegregationQuality> quality = combo(SegregationQuality.values());
        TextField collectedWeight = field("Actual kg", "");
        TextField citizenId = field("Household citizen ID (optional)", "");
        TableColumn<RouteStop, Boolean> collectedColumn = new TableColumn<>("Collected");
        collectedColumn.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cell.getValue().isCollected()));
        collectedColumn.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                setAlignment(Pos.CENTER);
                checkBox.setOnAction(event -> {
                    RouteStop stop = getTableRow() == null ? null : getTableRow().getItem();
                    if (stop == null || !checkBox.isSelected()) {
                        if (stop != null) checkBox.setSelected(stop.isCollected());
                        return;
                    }
                    runAction(() -> recordCollectorStop(stop, quality, collectedWeight, citizenId));
                    checkBox.setSelected(stop.isCollected());
                });
            }

            @Override
            protected void updateItem(Boolean collected, boolean empty) {
                super.updateItem(collected, empty);
                if (empty || collected == null) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(collected);
                    setGraphic(checkBox);
                }
            }
        });
        collectedColumn.setMinWidth(100);
        collectedColumn.setPrefWidth(110);
        routeTable.getColumns().add(collectedColumn);
        Button recordStop = new Button("Record selected stop");
        recordStop.setOnAction(event -> runAction(() -> {
            RouteStop selected = requireSelection(routeTable, "Select a route stop first.");
            recordCollectorStop(selected, quality, collectedWeight, citizenId);
        }));
        TextField obstruction = field("Blocked road / issue", "");
        Button reportBlocked = new Button("Report obstruction");
        reportBlocked.setOnAction(event -> runAction(() -> {
            Route route = collectorService.getTodayRoute(user.getId())
                    .orElseThrow(() -> new ValidationException("No route is assigned for today."));
            RouteStop selected = requireSelection(routeTable, "Select the blocked route stop first.");
            collectorService.reportBlockedRoad(route.getId(), selected.getSequence(), obstruction.getText());
            refreshCollectorData();
            status.setText("Remaining route stops replanned.");
        }));
        VBox route = new VBox(12, heading("Today's route", "Record each stop after collection, or report a blocked road to replan."),
                routeTable, formRow(quality, collectedWeight, citizenId, recordStop), formRow(obstruction, reportBlocked));
        route.setPadding(new Insets(20));
        VBox.setVgrow(routeTable, Priority.ALWAYS);
        tabs.getTabs().add(tab("Route", route));

        assignedComplaintsTable = table(
                column("Complaint", Complaint::getId),
                column("Type", complaint -> complaint.getComplaintType().getLabel()),
                column("Zone", Complaint::getZoneId),
                column("Status", complaint -> complaint.getStatus().name()),
                column("Description", Complaint::getDescription));
        TextField resolution = field("Resolution notes", "");
        Button resolve = new Button("Resolve selected complaint");
        resolve.setOnAction(event -> runAction(() -> {
            Complaint complaint = requireSelection(assignedComplaintsTable, "Select an assigned complaint first.");
            if (collectorService.resolveComplaint(complaint.getId(), resolution.getText())) {
                resolution.clear();
                refreshCollectorData();
                status.setText("Complaint marked resolved and the citizen was notified.");
            } else {
                throw new ValidationException("Complaint could not be updated.");
            }
        }));
        VBox complaints = new VBox(12, heading("Assigned complaints", "Resolve a complaint with a note that will be included in the citizen notification."),
                assignedComplaintsTable, formRow(resolution, resolve));
        complaints.setPadding(new Insets(20));
        VBox.setVgrow(assignedComplaintsTable, Priority.ALWAYS);
        tabs.getTabs().add(tab("Complaints", complaints));
        refreshCollectorData();
    }

    private void buildPickerWorkspace() {
        pickerStatusLabel = new Label();
        pickerEarningsLabel = new Label();
        pickerStatusLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #E5EEE8;");
        pickerEarningsLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #83D59D;");
        VBox overview = new VBox(14, heading("Waste picker dashboard", "Find recyclable pickup offers and track verified collection earnings."),
                infoPanel("Account verification", pickerStatusLabel), infoPanel("Lifetime earnings", pickerEarningsLabel));
        overview.setPadding(new Insets(24));
        tabs.getTabs().add(tab("Overview", overview));

        availableListingsTable = table(
                column("Listing", RecyclableListing::getId),
                column("Material", listing -> listing.getWasteType().getDisplayName()),
                column("Weight", listing -> String.format("%.1f kg", listing.getWeightKg())),
                column("Estimated value", listing -> String.format("₹%.0f", listing.getEstimatedValue())),
                column("Address", RecyclableListing::getAddress));
        pickerLatitudeField = field("Your latitude", "");
        pickerLongitudeField = field("Your longitude", "");
        Button updateLocation = new Button("Find nearby");
        updateLocation.setOnAction(event -> runAction(() -> {
            new Location(Double.parseDouble(pickerLatitudeField.getText().trim()),
                    Double.parseDouble(pickerLongitudeField.getText().trim()), "Picker current location");
            refreshPickerData();
            status.setText("Offers sorted by distance from your location.");
        }));
        Button claim = new Button("Claim selected offer");
        claim.setOnAction(event -> runAction(() -> {
            RecyclableListing selected = requireSelection(availableListingsTable, "Select an available listing first.");
            if (!pickerService.claimListing(selected.getId(), user.getId())) {
                throw new ValidationException("This offer is no longer available.");
            }
            refreshPickerData();
            status.setText("Offer claimed. Complete it from the My pickups tab after collection.");
        }));
        VBox available = new VBox(12, heading("Available offers", "Only verified picker accounts can claim recyclable pickups."),
            formRow(pickerLatitudeField, pickerLongitudeField, updateLocation), availableListingsTable, claim);
        available.setPadding(new Insets(20));
        VBox.setVgrow(availableListingsTable, Priority.ALWAYS);
        tabs.getTabs().add(tab("Available pickups", available));

        claimedListingsTable = table(
                column("Listing", RecyclableListing::getId),
                column("Material", listing -> listing.getWasteType().getDisplayName()),
                column("Listed kg", listing -> String.format("%.1f kg", listing.getWeightKg())),
                column("Address", RecyclableListing::getAddress),
                column("Status", listing -> listing.getStatus().name()));
        TextField actualWeight = field("Measured kg", "");
        Button complete = new Button("Complete selected pickup");
        complete.setOnAction(event -> runAction(() -> {
            RecyclableListing selected = requireSelection(claimedListingsTable, "Select a claimed pickup first.");
            if (!pickerService.completeCollection(selected.getId(), parseDouble(actualWeight, "Measured weight"), user.getId())) {
                throw new ValidationException("Pickup could not be completed.");
            }
            actualWeight.clear();
            refreshPickerData();
            status.setText("Pickup completed and earnings updated.");
        }));
        VBox claimed = new VBox(12, heading("My pickups", "Enter the measured weight after collection to finalize the pickup."), claimedListingsTable,
                formRow(actualWeight, complete));
        claimed.setPadding(new Insets(20));
        VBox.setVgrow(claimedListingsTable, Priority.ALWAYS);
        tabs.getTabs().add(tab("My pickups", claimed));
        refreshPickerData();
    }

    private void buildNotificationsTab() {
        notificationsTable = table(
                column("Received", notification -> notification.getCreatedAt().toString().replace('T', ' ')),
                column("Read", notification -> notification.isRead() ? "Read" : "Unread"),
                column("Title", Notification::getTitle),
                column("Message", Notification::getMessage));
        notificationCountLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #83D59D;");
        Button markSelected = new Button("Mark selected read");
        markSelected.setOnAction(event -> {
            Notification selected = notificationsTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                status.setText("Select a notification first.");
                return;
            }
            notificationService.markAsRead(selected.getId());
            refreshNotifications();
            status.setText("Notification marked read.");
        });
        Button markAll = new Button("Mark all read");
        markAll.setOnAction(event -> {
            notificationService.markAllAsRead(user.getId());
            refreshNotifications();
            status.setText("All notifications marked read.");
        });
        VBox content = new VBox(12, heading("Notifications", "Updates related to your collection and account."),
                notificationCountLabel, formRow(markSelected, markAll), notificationsTable);
        content.setPadding(new Insets(20));
        VBox.setVgrow(notificationsTable, Priority.ALWAYS);
        tabs.getTabs().add(tab("Notifications", content));
        refreshNotifications();
    }

    private void refreshNotifications() {
        if (notificationsTable == null) return;
        notificationsTable.setItems(FXCollections.observableArrayList(notificationService.getUserNotifications(user.getId())));
        notificationCountLabel.setText(notificationService.getUnreadCount(user.getId()) + " unread");
    }

    private void refreshAll() {
        switch (user.getRole()) {
            case CITIZEN -> refreshCitizenData();
            case COLLECTOR -> refreshCollectorData();
            case WASTE_PICKER -> refreshPickerData();
            default -> { }
        }
        status.setText("Data refreshed.");
    }

    private void refreshCitizenData() {
        Citizen citizen = citizenService.getCitizen(user.getId()).orElse(null);
        if (citizen == null) {
            nextPickupLabel.setText("Household profile unavailable");
            pointsLabel.setText("0 points");
        } else {
            pointsLabel.setText(citizen.getPointsBalance() + " points");
            PickupRequest next = citizenService.getCitizenPickups(user.getId()).stream()
                    .filter(request -> !request.getPreferredSlot().isBefore(LocalDate.now()))
                    .filter(request -> request.getStatus() != com.wasteconnect.model.PickupStatus.CANCELLED
                            && request.getStatus() != com.wasteconnect.model.PickupStatus.COMPLETED)
                    .min(Comparator.comparing(PickupRequest::getPreferredSlot)).orElse(null);
            nextPickupLabel.setText(next == null ? "No upcoming collection scheduled" :
                    next.getPreferredSlot() + "  /  " + next.getWasteType().getDisplayName() + "  /  " + next.getStatus());
        }
        if (pickupsTable != null) pickupsTable.setItems(FXCollections.observableArrayList(citizenService.getCitizenPickups(user.getId())));
        if (rewardsTable != null) rewardsTable.setItems(FXCollections.observableArrayList(citizenService.getRewardTransactions(user.getId())));
        if (citizenComplaintsTable != null) citizenComplaintsTable.setItems(FXCollections.observableArrayList(citizenService.getCitizenComplaints(user.getId())));
        if (citizenListingsTable != null) citizenListingsTable.setItems(FXCollections.observableArrayList(citizenService.getCitizenListings(user.getId())));
    }

    private void refreshCollectorData() {
        if (routeTable != null) {
            List<RouteStop> stops = collectorService.getTodayRoute(user.getId()).map(Route::getStops).orElse(List.of());
            routeTable.setItems(FXCollections.observableArrayList(stops));
        }
        if (assignedComplaintsTable != null) assignedComplaintsTable.setItems(
                FXCollections.observableArrayList(collectorService.getAssignedComplaints(user.getId())));
    }

        private void recordCollectorStop(RouteStop stop, ComboBox<SegregationQuality> quality,
                         TextField collectedWeight, TextField citizenId) {
        Route route = collectorService.getTodayRoute(user.getId())
            .orElseThrow(() -> new ValidationException("No route is assigned for today."));
        collectorService.recordStopCollected(route.getId(), stop.getSequence(), stop.getStopId(), quality.getValue(),
            parseDouble(collectedWeight, "Actual weight"), citizenId.getText());
        refreshCollectorData();
        status.setText("Stop recorded. Household points and segregation score update when a citizen ID is supplied.");
        }

    private void refreshPickerData() {
        WastePicker picker = pickerService.getPicker(user.getId()).orElse(null);
        if (picker != null) {
            pickerStatusLabel.setText(picker.isVerified() ? "Verified and active" : "Pending municipal verification");
            pickerEarningsLabel.setText(String.format("₹%.2f  /  %.1f kg", picker.getTotalEarnings(), picker.getTotalKgCollected()));
        }
        if (availableListingsTable != null) {
            try {
                if (pickerLatitudeField.getText().isBlank() || pickerLongitudeField.getText().isBlank()) {
                    availableListingsTable.setItems(FXCollections.observableArrayList(
                            pickerService.getNearbyAvailableListings(null)));
                    status.setText("Enter your coordinates to sort available pickups by distance.");
                    return;
                }
                double latitude = Double.parseDouble(pickerLatitudeField.getText().trim());
                double longitude = Double.parseDouble(pickerLongitudeField.getText().trim());
                Location pickerLocation = new Location(latitude, longitude, "Picker current location");
                availableListingsTable.setItems(FXCollections.observableArrayList(
                        pickerService.getNearbyAvailableListings(pickerLocation)));
            } catch (RuntimeException exception) {
                availableListingsTable.setItems(FXCollections.observableArrayList());
                status.setText(exception.getMessage() == null ? "Enter valid location coordinates." : exception.getMessage());
            }
        }
        if (claimedListingsTable != null) claimedListingsTable.setItems(FXCollections.observableArrayList(
                pickerService.getPickerActiveListings(user.getId()).stream()
                        .filter(listing -> listing.getStatus() == ListingStatus.CLAIMED).toList()));
    }

    private void subscribeToEvents() {
        EventBus bus = EventBus.getInstance();
        if (user.getRole() == UserRole.CITIZEN) {
            eventSubscriptions.add(bus.subscribe(EventBus.NotificationCreated.class, event -> {
                if (user.getId().equals(event.userId())) refreshFromEvent(this::refreshNotifications);
            }));
            eventSubscriptions.add(bus.subscribe(EventBus.HouseholdCollected.class, event -> {
                if (user.getId().equals(event.householdId())) refreshFromEvent(this::refreshCitizenData);
            }));
            eventSubscriptions.add(bus.subscribe(EventBus.PointsAwarded.class, event -> {
                if (user.getId().equals(event.citizenId())) refreshFromEvent(this::refreshCitizenData);
            }));
            eventSubscriptions.add(bus.subscribe(EventBus.PickupRequested.class, event -> {
                if (user.getId().equals(event.citizenId())) refreshFromEvent(this::refreshCitizenData);
            }));
            eventSubscriptions.add(bus.subscribe(EventBus.ComplaintRaised.class, event -> {
                if (user.getId().equals(event.citizenId())) refreshFromEvent(this::refreshCitizenData);
            }));
            eventSubscriptions.add(bus.subscribe(EventBus.ComplaintResolved.class, event -> {
                if (user.getId().equals(event.citizenId())) refreshFromEvent(this::refreshCitizenData);
            }));
            eventSubscriptions.add(bus.subscribe(EventBus.ListingCreated.class, event -> {
                if (user.getId().equals(event.citizenId())) refreshFromEvent(this::refreshCitizenData);
            }));
            eventSubscriptions.add(bus.subscribe(EventBus.ListingAccepted.class, event -> {
                if (user.getId().equals(event.citizenId())) refreshFromEvent(this::refreshCitizenData);
            }));
            eventSubscriptions.add(bus.subscribe(EventBus.ListingCompleted.class, event -> {
                if (user.getId().equals(event.citizenId())) refreshFromEvent(this::refreshCitizenData);
            }));
        } else if (user.getRole() == UserRole.COLLECTOR) {
            eventSubscriptions.add(bus.subscribe(EventBus.NotificationCreated.class, event -> {
                if (user.getId().equals(event.userId())) refreshFromEvent(this::refreshNotifications);
            }));
            eventSubscriptions.add(bus.subscribe(EventBus.RouteGenerated.class, event -> {
                if (user.getId().equals(event.collectorId())) refreshFromEvent(this::refreshCollectorData);
            }));
            eventSubscriptions.add(bus.subscribe(EventBus.RouteReplanned.class, event -> refreshFromEvent(this::refreshCollectorData)));
            eventSubscriptions.add(bus.subscribe(EventBus.ComplaintAssigned.class, event -> {
                if (user.getId().equals(event.collectorId())) refreshFromEvent(this::refreshCollectorData);
            }));
        } else if (user.getRole() == UserRole.WASTE_PICKER) {
            eventSubscriptions.add(bus.subscribe(EventBus.NotificationCreated.class, event -> {
                if (user.getId().equals(event.userId())) refreshFromEvent(this::refreshNotifications);
            }));
            eventSubscriptions.add(bus.subscribe(EventBus.PickerVerified.class, event -> {
                if (user.getId().equals(event.pickerId())) refreshFromEvent(this::refreshPickerData);
            }));
            eventSubscriptions.add(bus.subscribe(EventBus.ListingCreated.class, event -> refreshFromEvent(this::refreshPickerData)));
            eventSubscriptions.add(bus.subscribe(EventBus.ListingAccepted.class, event -> refreshFromEvent(this::refreshPickerData)));
            eventSubscriptions.add(bus.subscribe(EventBus.ListingCompleted.class, event -> refreshFromEvent(this::refreshPickerData)));
        }
    }

    private void refreshFromEvent(Runnable refreshAction) {
        Platform.runLater(() -> {
            refreshAction.run();
            status.setText("Updated from a new service event.");
        });
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

    private void runAction(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            status.setText(exception.getMessage() == null ? "Action could not be completed." : exception.getMessage());
        }
    }

    private static <T> T requireSelection(TableView<T> table, String message) {
        T selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) throw new ValidationException(message);
        return selected;
    }

    private static double parseDouble(TextField field, String label) {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (RuntimeException exception) {
            throw new ValidationException(label + " must be a number.");
        }
    }

    private static int parseInt(TextField field, String label) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (RuntimeException exception) {
            throw new ValidationException(label + " must be a whole number.");
        }
    }

    @SafeVarargs
    private static <T> TableView<T> table(TableColumn<T, String>... columns) {
        TableView<T> table = new TableView<>();
        table.getColumns().addAll(columns);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No records available"));
        return table;
    }

    private static <T> TableColumn<T, String> column(String title, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private static <T> ComboBox<T> combo(T[] values) {
        ComboBox<T> combo = new ComboBox<>(FXCollections.observableArrayList(values));
        combo.getSelectionModel().selectFirst();
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private static TextField field(String prompt, String initialValue) {
        TextField field = new TextField(initialValue);
        field.setPromptText(prompt);
        field.setPrefWidth(170);
        return field;
    }

    private static HBox formRow(Node... nodes) {
        HBox row = new HBox(10, nodes);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static VBox heading(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #E5EEE8;");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-text-fill: #A2B1A7;");
        return new VBox(5, titleLabel, subtitleLabel);
    }

    private static VBox infoPanel(String title, Node value) {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #A2B1A7;");
        VBox panel = new VBox(9, label, value);
        panel.setPadding(new Insets(18));
        panel.setMaxWidth(620);
        panel.setStyle("-fx-background-color: #19241E; -fx-background-radius: 8; -fx-border-color: #35463B; -fx-border-radius: 8;");
        return panel;
    }

    private static Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private static String displayRole(UserRole role) {
        return switch (role) {
            case CITIZEN -> "Household";
            case COLLECTOR -> "Waste collector";
            case ADMIN -> "Administrator";
            case WASTE_PICKER -> "Waste picker";
        };
    }
}