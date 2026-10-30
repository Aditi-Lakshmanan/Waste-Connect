package com.wasteconnect.ui;

import com.wasteconnect.dao.DatabaseConnection;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    private final SceneManager sceneManager = new SceneManager();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        DatabaseConnection.getInstance();
        sceneManager.initialize(stage);
        sceneManager.showLogin();
        stage.setMinWidth(1200);
        stage.setMinHeight(760);
        stage.setTitle("WasteConnect – Smart Waste Collection Management System");
        stage.show();
    }
}
