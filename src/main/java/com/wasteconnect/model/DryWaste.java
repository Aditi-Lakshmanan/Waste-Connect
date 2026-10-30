package com.wasteconnect.model;

/**
 * INHERITANCE: DryWaste represents recyclable paper, plastic, glass and metals.
 */
public class DryWaste extends Waste {

    public DryWaste(double weightKg) {
        super(null, WasteType.DRY, weightKg);
    }

    public DryWaste(String id, double weightKg) {
        super(id, WasteType.DRY, weightKg);
    }

    // POLYMORPHISM: Dry recyclables award 10 reward points per kg
    @Override
    public int calculateRewardPoints(double kg) {
        if (kg <= 0) return 0;
        return (int) Math.round(kg * 10.0);
    }

    // POLYMORPHISM: Industrial recycling handling instructions
    @Override
    public String getHandlingInstructions() {
        return "Rinse containers, flatten cardboard boxes, and keep dry. Routed to Material Recovery Facility (MRF) for optical sorting and baling.";
    }
}
