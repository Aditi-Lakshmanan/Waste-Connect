package com.wasteconnect.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Centralized application logging facility configuring java.util.logging with file output to logs/app.log.
 */
public final class AppLogger {
    private static final Logger ROOT_LOGGER = Logger.getLogger("com.wasteconnect");
    private static boolean initialized = false;

    static {
        initialize();
    }

    private AppLogger() {}

    public static synchronized void initialize() {
        if (initialized) return;
        try {
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            FileHandler fileHandler = new FileHandler("logs/app.log", 5 * 1024 * 1024, 3, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);

            ROOT_LOGGER.addHandler(fileHandler);
            ROOT_LOGGER.setLevel(Level.INFO);
            ROOT_LOGGER.setUseParentHandlers(true);
            initialized = true;
        } catch (IOException e) {
            System.err.println("Failed to initialize file logger: " + e.getMessage());
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }

    public static void info(String message) {
        ROOT_LOGGER.info(message);
    }

    public static void warning(String message) {
        ROOT_LOGGER.warning(message);
    }

    public static void severe(String message, Throwable t) {
        ROOT_LOGGER.log(Level.SEVERE, message, t);
    }
}
