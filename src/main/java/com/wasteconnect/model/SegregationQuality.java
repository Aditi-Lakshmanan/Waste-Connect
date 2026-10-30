package com.wasteconnect.model;

/**
 * Enumeration of domestic waste segregation compliance levels scored at collection time.
 */
public enum SegregationQuality {
    GOOD("Good Segregation", 1.0, 10, "+10 pts, streak maintained", "#2E7D32"),
    PARTIAL("Partial Segregation", 0.5, 3, "+3 pts, neutral streak", "#F57C00"),
    MIXED("Unsegregated / Mixed", 0.0, -5, "-5 pts, streak reset", "#D32F2F");

    private final String label;
    private final double scoreMultiplier;
    private final int bonusPoints;
    private final String description;
    private final String colorHex;

    SegregationQuality(String label, double scoreMultiplier, int bonusPoints, String description, String colorHex) {
        this.label = label;
        this.scoreMultiplier = scoreMultiplier;
        this.bonusPoints = bonusPoints;
        this.description = description;
        this.colorHex = colorHex;
    }

    public String getLabel() {
        return label;
    }

    public double getScoreMultiplier() {
        return scoreMultiplier;
    }

    public int getBonusPoints() {
        return bonusPoints;
    }

    public String getDescription() {
        return description;
    }

    public String getColorHex() {
        return colorHex;
    }
}
