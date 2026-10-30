package com.wasteconnect.exception;

/**
 * INHERITANCE: Specific domain exception triggered when a bin reaches or exceeds 100% fill level.
 */
public class BinOverflowException extends WasteConnectException {
    private final String binId;
    private final double currentLevel;

    public BinOverflowException(String binId, double currentLevel) {
        super(String.format("Critical Alert: Bin %s has overflowed! Current fill level: %.1f%%", binId, currentLevel));
        this.binId = binId;
        this.currentLevel = currentLevel;
    }

    public String getBinId() {
        return binId;
    }

    public double getCurrentLevel() {
        return currentLevel;
    }
}
