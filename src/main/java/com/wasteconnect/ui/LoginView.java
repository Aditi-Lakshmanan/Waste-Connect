package com.wasteconnect.ui;

import com.wasteconnect.exception.AuthenticationException;
import com.wasteconnect.exception.ValidationException;
import com.wasteconnect.model.User;
import com.wasteconnect.model.UserRole;
import com.wasteconnect.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

public class LoginView extends BorderPane {
    private final SceneManager sceneManager;
    private final AuthService authService = new AuthService();

    public LoginView(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        setPrefSize(1200, 760);
        setStyle("-fx-background: #101713;");
        buildLayout();
    }

    private void buildLayout() {
        VBox leftPanel = new VBox(16);
        leftPanel.setAlignment(Pos.CENTER_LEFT);
        leftPanel.setPadding(new Insets(50, 60, 50, 60));
        leftPanel.setStyle("-fx-background-color: #183123; -fx-text-fill: white;");

        HBox logoRow = new HBox(14);
        logoRow.setAlignment(Pos.CENTER_LEFT);
        Circle circle = new Circle(24, Color.web("#E8F5E9"));
        Polygon leftArrow = new Polygon(20, 10, 44, 22, 20, 34);
        leftArrow.setFill(Color.web("#2E7D32"));
        Polygon rightArrow = new Polygon(40, 10, 64, 22, 40, 34);
        rightArrow.setFill(Color.web("#2E7D32"));
        Polygon topArrow = new Polygon(15, 22, 35, 8, 35, 36);
        topArrow.setFill(Color.web("#00897B"));
        Rectangle pinBase = new Rectangle(18, 28, Color.web("#E8F5E9"));
        pinBase.setArcWidth(10);
        pinBase.setArcHeight(10);

        HBox iconBox = new HBox(4, circle, topArrow, leftArrow, rightArrow, pinBase);
        iconBox.setAlignment(Pos.CENTER);
        Label brand = new Label("WasteConnect");
        brand.setTextFill(Color.WHITE);
        brand.setFont(Font.font("Segoe UI", FontWeight.BOLD, 38));
        logoRow.getChildren().addAll(iconBox, brand);

        Label tagline = new Label("Smarter collection. Cleaner cities.");
        tagline.setTextFill(Color.web("#E8F5E9"));
        tagline.setFont(Font.font("Segoe UI", 20));

        Label welcome = new Label("City operations dashboard");
        welcome.setTextFill(Color.web("#DDEEE0"));
        welcome.setFont(Font.font("Segoe UI", 16));

        leftPanel.getChildren().addAll(logoRow, tagline, welcome);

        VBox formPanel = new VBox(18);
        formPanel.setAlignment(Pos.CENTER);
        formPanel.setPadding(new Insets(60, 80, 60, 80));
        formPanel.setMaxWidth(420);

        Label title = new Label("Sign in");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #EDF4EF;");

        Label subTitle = new Label("Access the smart waste network");
        subTitle.setTextFill(Color.web("#A2B1A7"));
        subTitle.setStyle("-fx-font-size: 14px;");

        ComboBox<UserRole> roleSelector = new ComboBox<>();
        roleSelector.getItems().setAll(UserRole.values());
        roleSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserRole role) {
                if (role == null) return "Select account type";
                return switch (role) {
                    case CITIZEN -> "Household / Citizen";
                    case COLLECTOR -> "Waste Collector";
                    case ADMIN -> "Administrator";
                    case WASTE_PICKER -> "Waste Picker";
                };
            }

            @Override
            public UserRole fromString(String value) {
                return null;
            }
        });
        roleSelector.setValue(UserRole.ADMIN);
        roleSelector.setMaxWidth(Double.MAX_VALUE);
        roleSelector.setPrefHeight(42);

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.setPrefHeight(42);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setPrefHeight(42);

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.web("#FF8A80"));
        errorLabel.setWrapText(true);
        errorLabel.setMinHeight(24);

        Button loginButton = new Button("Login");
        loginButton.setPrefHeight(46);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;");
        loginButton.setOnAction(event -> {
            errorLabel.setText("");
            try {
                User user = authService.login(emailField.getText(), passwordField.getText(), roleSelector.getValue());
                sceneManager.showDashboard(user);
            } catch (ValidationException | AuthenticationException ex) {
                errorLabel.setText(ex.getMessage());
            }
        });

        VBox fields = new VBox(12, roleSelector, emailField, passwordField, errorLabel, loginButton);
        fields.setFillWidth(true);

        HBox hintRow = new HBox();
        hintRow.setAlignment(Pos.CENTER_LEFT);
        Label hintTitle = new Label("Demo accounts:");
        hintTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #DDE8E0;");
        Label hint = new Label("Select an account type to view its demo sign-in.");
        hint.setTextFill(Color.web("#A2B1A7"));
        hintRow.getChildren().addAll(hintTitle, hint);

        roleSelector.setOnAction(event -> {
            String accountHint = switch (roleSelector.getValue()) {
                case ADMIN -> "admin@wasteconnect.com / Admin@123";
                case CITIZEN -> "citizen1@wasteconnect.com / Citizen@123";
                case COLLECTOR -> "collector1@wasteconnect.com / Collect@123";
                case WASTE_PICKER -> "picker1@wasteconnect.com / Picker@123";
            };
            hint.setText(accountHint);
        });
        roleSelector.fireEvent(new javafx.event.ActionEvent());

        formPanel.getChildren().addAll(title, subTitle, fields, hintRow);

        setLeft(leftPanel);
        setCenter(formPanel);
    }
}
