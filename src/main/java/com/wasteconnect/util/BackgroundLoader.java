package com.wasteconnect.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Utility to run a {@link Callable} on a background thread and deliver the result on the
 * JavaFX Application Thread. UI data-fetching should go through this helper so the UI
 * remains responsive and the code stays DRY.
 */
public class BackgroundLoader {
    public static <T> void load(Callable<T> work, Consumer<T> onSuccess) {
        load(work, onSuccess, ex -> AppLogger.severe("Background load failed: " + ex.getMessage(), ex));
    }

    public static <T> void load(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(ev -> Platform.runLater(() -> onSuccess.accept(task.getValue())));
        task.setOnFailed(ev -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            if (onError != null) {
                onError.accept(ex);
            } else {
                AppLogger.severe("Background load failed: " + ex.getMessage(), ex);
            }
        }));
        Thread thread = new Thread(task, "bg-loader-" + System.nanoTime());
        thread.setDaemon(true);
        thread.start();
    }
}
