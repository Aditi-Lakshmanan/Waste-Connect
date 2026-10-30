package com.wasteconnect.model;

/**
 * INHERITANCE: WetWaste represents biodegradable organic waste.
 */
public class WetWaste extends Waste {

    public WetWaste(double weightKg) {
        super(null, WasteType.WET, weightKg);
    }

    public WetWaste(String id, double weightKg) {
        super(id, WasteType.WET, weightKg);
    }

    // POLYMORPHISM: Organic waste awards 5 reward points per kg, rounded up
    @Override
    public int calculateRewardPoints(double kg) {
        if (kg <= 0) return 0;
        return (int) Math.round(kg * 5.0);
    }

    // POLYMORPHISM: Specific biological handling instructions
    @Override
    public String getHandlingInstructions() {
        return "Keep separate from plastic liners. Deposit in green bins. Dispatched for anaerobic digestion and organic composting.";
    }
}
