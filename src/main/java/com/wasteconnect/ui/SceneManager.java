package com.wasteconnect.ui;

import com.wasteconnect.model.User;
import com.wasteconnect.model.UserRole;
import com.wasteconnect.service.AuthService;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {
    private Stage stage;

    public void initialize(Stage stage) {
        this.stage = stage;
    }

    public void showLogin() {
        if (stage == null) {
            throw new IllegalStateException("SceneManager has not been initialized with a Stage.");
        }
        LoginView loginView = new LoginView(this);
        Scene scene = new Scene(loginView, 1200, 760);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
    }

    public void showDashboard(User user) {
        if (stage == null) {
            throw new IllegalStateException("SceneManager has not been initialized with a Stage.");
        }
        javafx.scene.Parent view = user.getRole() == UserRole.ADMIN
            ? new AdminWorkspaceView(this, user)
            : new RoleWorkspaceView(this, user);
        Scene scene = new Scene(view, 1200, 760);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
    }

    public void showLogout() {
        AuthService.logout();
        showLogin();
    }
}
