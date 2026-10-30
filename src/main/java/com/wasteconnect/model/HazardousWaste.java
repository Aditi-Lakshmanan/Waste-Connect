package com.wasteconnect.model;

/**
 * INHERITANCE: HazardousWaste represents biomedical, toxic, sanitary, and chemical items.
 */
public class HazardousWaste extends Waste {

    public HazardousWaste(double weightKg) {
        super(null, WasteType.HAZARDOUS, weightKg);
    }

    public HazardousWaste(String id, double weightKg) {
        super(id, WasteType.HAZARDOUS, weightKg);
    }

    // POLYMORPHISM: Hazardous waste carries no commercial reward; fixed safety disposal compliance credit of 2 points
    @Override
    public int calculateRewardPoints(double kg) {
        return kg > 0 ? 2 : 0;
    }

    // POLYMORPHISM: High-priority hazardous protocol
    @Override
    public String getHandlingInstructions() {
        return "HAZARD WARNING: Wear nitrile gloves and masks. Wrap sharp objects. Place in leak-proof red hazard bags. Transport to authorized biomedical incinerator.";
    }
}
