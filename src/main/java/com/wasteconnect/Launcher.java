package com.wasteconnect;

import com.wasteconnect.ui.MainApp;

/**
 * Standard main class entry point that does not extend {@link javafx.application.Application}.
 * This avoids the modular JavaFX runtime requirement error when executing from packaged JARs or IDEs.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
