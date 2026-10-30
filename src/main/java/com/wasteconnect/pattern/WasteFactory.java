package com.wasteconnect.pattern;

import com.wasteconnect.model.*;

/**
 * CO2 & DESIGN PATTERN: Factory pattern for generating strongly-typed polymorphic Waste instances.
 */
public final class WasteFactory {

    private WasteFactory() {}

    // POLYMORPHISM: Produces concrete Waste subtypes dynamically based on classification
    public static Waste createWaste(WasteType type, double weightKg) {
        if (type == null) {
            throw new IllegalArgumentException("WasteType cannot be null");
        }
        return switch (type) {
            case WET -> new WetWaste(weightKg);
            case DRY -> new DryWaste(weightKg);
            case HAZARDOUS -> new HazardousWaste(weightKg);
            case E_WASTE -> new EWaste(weightKg);
        };
    }

    public static Waste createWaste(String id, WasteType type, double weightKg) {
        if (type == null) {
            throw new IllegalArgumentException("WasteType cannot be null");
        }
        return switch (type) {
            case WET -> new WetWaste(id, weightKg);
            case DRY -> new DryWaste(id, weightKg);
            case HAZARDOUS -> new HazardousWaste(id, weightKg);
            case E_WASTE -> new EWaste(id, weightKg);
        };
    }
}
