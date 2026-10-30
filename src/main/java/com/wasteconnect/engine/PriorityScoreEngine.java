package com.wasteconnect.engine;

import com.wasteconnect.dao.SettingDao;
import com.wasteconnect.dao.SettingDaoImpl;
import com.wasteconnect.model.Bin;

/**
 * Novelty Engine: Computes real-time dynamic urgency score for IoT waste bins.
 * Formula: Priority = w1*fillLevel + w2*daysSinceLastPickup + w3*openComplaints + w4*wasteTypeUrgency
 */
public class PriorityScoreEngine {
    private final SettingDao settingDao;

    public PriorityScoreEngine() {
        this(new SettingDaoImpl());
    }

    public PriorityScoreEngine(SettingDao settingDao) {
        this.settingDao = settingDao;
    }

    /**
     * Calculates the normalized priority score (0.0 to 100.0) for a given bin.
     */
    public double calculatePriority(Bin bin) {
        if (bin == null) return 0.0;

        double w1 = settingDao.getDouble("w1_fill_level", 0.45);
        double w2 = settingDao.getDouble("w2_days_since_pickup", 0.25);
        double w3 = settingDao.getDouble("w3_complaints", 0.15);
        double w4 = settingDao.getDouble("w4_waste_urgency", 0.15);

        // Normalize weights to sum to 1.0
        double sumWeights = w1 + w2 + w3 + w4;
        if (sumWeights <= 0) sumWeights = 1.0;
        w1 /= sumWeights;
        w2 /= sumWeights;
        w3 /= sumWeights;
        w4 /= sumWeights;

        // Sub-component normalizations (each 0-100)
        double fillTerm = bin.getCurrentFillPercentage(); // 0 to 100
        double daysTerm = Math.min(100.0, bin.getDaysSinceLastPickup() * 20.0); // 5 days = 100
        double complaintTerm = Math.min(100.0, bin.getOpenComplaintsCount() * 33.3); // 3 complaints = 100
        double urgencyTerm = Math.min(100.0, bin.getWasteType().getUrgencyWeight() * 45.0);

        double rawScore = (w1 * fillTerm) + (w2 * daysTerm) + (w3 * complaintTerm) + (w4 * urgencyTerm);
        double finalScore = Math.max(0.0, Math.min(100.0, Math.round(rawScore * 10.0) / 10.0));
        bin.setCalculatedPriority(finalScore);
        return finalScore;
    }
}
