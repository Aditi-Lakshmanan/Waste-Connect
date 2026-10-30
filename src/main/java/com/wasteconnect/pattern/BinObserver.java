package com.wasteconnect.pattern;

import com.wasteconnect.model.Bin;
import com.wasteconnect.model.BinStatus;

/**
 * CO2 & DESIGN PATTERN: Observer interface for receiving real-time IoT smart bin telemetry events.
 */
public interface BinObserver {
    void onBinStatusChanged(Bin bin, BinStatus oldStatus, BinStatus newStatus);
    void onBinFillUpdated(Bin bin, double oldFill, double newFill);
}
