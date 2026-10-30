package com.wasteconnect.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility to run a {@link Supplier} on a background thread and deliver the result on the
 * JavaFX Application Thread. All UI data‑fetching should go through this helper so the UI
 * remains responsive and the code stays DRY.
 */
public class BackgroundLoader {
    public static <T> void load(Supplier<T> work, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.get();
            }
        };
        task.setOnSucceeded(ev -> Platform.runLater(() -> onSuccess.accept(task.getValue())));
        task.setOnFailed(ev -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            AppLogger.severe("Background load failed: " + ex.getMessage(), ex);
        }));
        new Thread(task, "bg-loader-" + System.nanoTime()).start();
    }
}
