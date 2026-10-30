package com.wasteconnect.pattern;

import com.wasteconnect.model.Bin;
import com.wasteconnect.model.BinStatus;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CO2 & DESIGN PATTERN: Thread-safe Observable subject maintaining subscriber registries for bin telemetry.
 */
public class BinObservable {
    private final List<BinObserver> observers = new CopyOnWriteArrayList<>();

    public void addObserver(BinObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(BinObserver observer) {
        observers.remove(observer);
    }

    public void notifyStatusChanged(Bin bin, BinStatus oldStatus, BinStatus newStatus) {
        for (BinObserver o : observers) {
            try {
                o.onBinStatusChanged(bin, oldStatus, newStatus);
            } catch (Exception e) {
                System.err.println("Error in BinObserver notification: " + e.getMessage());
            }
        }
    }

    public void notifyFillUpdated(Bin bin, double oldFill, double newFill) {
        for (BinObserver o : observers) {
            try {
                o.onBinFillUpdated(bin, oldFill, newFill);
            } catch (Exception e) {
                System.err.println("Error in BinObserver notification: " + e.getMessage());
            }
        }
    }
}
