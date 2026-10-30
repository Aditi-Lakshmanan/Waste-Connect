package com.wasteconnect.ui;

import com.wasteconnect.model.User;
import com.wasteconnect.model.UserRole;
import com.wasteconnect.model.WasteAnalytics;
import com.wasteconnect.service.AdminService;
import com.wasteconnect.service.CitizenService;
import com.wasteconnect.service.CollectorService;
import com.wasteconnect.service.WastePickerService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class DashboardView extends BorderPane {
    private final SceneManager sceneManager;
    private final User user;

    public DashboardView(SceneManager sceneManager, User user) {
        this.sceneManager = sceneManager;
        this.user = user;
        setPadding(new Insets(20));
        setStyle("-fx-background: #101713;");
        buildLayout();
    }

    private void buildLayout() {
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.setStyle("-fx-background-color: #19241E; -fx-background-radius: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 1);");

        Label appName = new Label("WasteConnect");
        appName.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #72CF91;");

        Label userBadge = new Label(user.getRole().name());
        userBadge.setStyle("-fx-background-color: #263B30; -fx-padding: 6 10 6 10; -fx-background-radius: 12; -fx-text-fill: #8DD7A5; -fx-font-weight: bold;");

        Label nameLabel = new Label(user.getName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #E5EEE8;");

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> sceneManager.showLogout());
        logoutButton.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        topBar.getChildren().addAll(appName, spacer, nameLabel, userBadge, logoutButton);

        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 0, 0, 0));

        Label heading = new Label(getRoleTitle());
        heading.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #E5EEE8;");

        HBox stats = new HBox(16);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
            createStatCard("Status", "Online"),
            createStatCard("Role", user.getRole().name()),
            createStatCard("Email", user.getEmail())
        );

        ListView<String> overview = new ListView<>();
        overview.setPrefHeight(320);
        overview.getItems().addAll(buildOverviewItems());

        content.getChildren().addAll(heading, stats, overview);
        setTop(topBar);
        setCenter(content);
    }

    private VBox createStatCard(String title, String value) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color: #19241E; -fx-background-radius: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 1);");

        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Color.web("#A2B1A7"));
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #E5EEE8;");
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private String getRoleTitle() {
        return switch (user.getRole()) {
            case CITIZEN -> "Citizen dashboard";
            case COLLECTOR -> "Collector operations";
            case ADMIN -> "Admin overview";
            case WASTE_PICKER -> "Waste picker dashboard";
            default -> "Operations dashboard";
        };
    }

    private java.util.List<String> buildOverviewItems() {
        return switch (user.getRole()) {
            case CITIZEN -> {
                CitizenService service = new CitizenService();
                var citizen = service.getCitizen(user.getId()).orElse(null);
                var guideSize = CitizenService.getSegregationGuide().size();
                yield java.util.List.of(
                    "Citizen ID: " + user.getId(),
                    "Points balance: " + (citizen != null ? citizen.getPointsBalance() : 0),
                    "Segregation score: " + (citizen != null ? citizen.getSegregationScore() : 0.0),
                    "Segregation guide entries: " + guideSize,
                    "Open complaints: " + service.getCitizenComplaints(user.getId()).size()
                );
            }
            case COLLECTOR -> {
                CollectorService service = new CollectorService();
                yield java.util.List.of(
                    "Collector ID: " + user.getId(),
                    "Today route: " + service.getTodayRoute(user.getId()).map(route -> route.getId()).orElse("Not assigned"),
                    "Assigned complaints: " + service.getAssignedComplaints(user.getId()).size(),
                    "Route planning is active and synchronized to live data."
                );
            }
            case ADMIN -> {
                AdminService service = new AdminService();
                WasteAnalytics analytics = service.getMunicipalAnalytics();
                yield java.util.List.of(
                    "Admin dashboard active",
                    "Collection efficiency: " + String.format("%.1f%%", analytics.collectionEfficiencyPct()),
                    "Average segregation rate: " + String.format("%.1f%%", analytics.averageSegregationRatePct()),
                    "Complaints resolved: " + String.format("%.1f%%", analytics.complaintsResolvedPct()),
                    "Hotspots: " + service.detectHotspots().size()
                );
            }
            case WASTE_PICKER -> {
                WastePickerService service = new WastePickerService();
                yield java.util.List.of(
                    "Waste picker ID: " + user.getId(),
                    "Nearby listings: " + service.getNearbyAvailableListings(null).size(),
                    "Active claims: " + service.getPickerActiveListings(user.getId()).size(),
                    "Pickup route is synchronized to your verified status."
                );
            }
            default -> java.util.List.of("Operations dashboard ready");
        };
    }
}
