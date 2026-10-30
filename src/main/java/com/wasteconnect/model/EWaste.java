package com.wasteconnect.model;

/**
 * INHERITANCE: EWaste represents electrical and electronic appliances containing precious or heavy metals.
 */
public class EWaste extends Waste {

    public EWaste(double weightKg) {
        super(null, WasteType.E_WASTE, weightKg);
    }

    public EWaste(String id, double weightKg) {
        super(id, WasteType.E_WASTE, weightKg);
    }

    // POLYMORPHISM: Electronics yield high reward value (20 points per kg)
    @Override
    public int calculateRewardPoints(double kg) {
        if (kg <= 0) return 0;
        return (int) Math.round(kg * 20.0);
    }

    // POLYMORPHISM: Safe electronics dismantling instructions
    @Override
    public String getHandlingInstructions() {
        return "Do not crush lithium batteries or cathode-ray tubes. Store in moisture-free container. Dispatched to certified WEEE dismantling and rare-metal refining plant.";
    }
}
